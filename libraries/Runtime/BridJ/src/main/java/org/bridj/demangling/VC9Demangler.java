package org.bridj.demangling;

import org.bridj.ann.Convention.Style;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bridj.NativeLibrary;
import org.bridj.demangling.Demangler.ClassRef;
import org.bridj.demangling.Demangler.DemanglingException;
import org.bridj.demangling.Demangler.MemberRef;
import org.bridj.demangling.Demangler.NamespaceRef;
import org.bridj.demangling.Demangler.Ident;
import org.bridj.demangling.Demangler.IdentLike;
import org.bridj.demangling.Demangler.TypeRef;
import org.bridj.demangling.Demangler.SpecialName;
import org.bridj.CLong;
import org.bridj.ann.Convention;
import java.math.BigInteger;
import java.util.Collection;

public class VC9Demangler extends Demangler {
	public VC9Demangler(NativeLibrary library, String str) {
		super(library, str);
	}

    private AccessLevelAndStorageClass parseAccessLevelAndStorageClass() throws DemanglingException {
        AccessLevelAndStorageClass ac = new AccessLevelAndStorageClass();
        switch (consumeChar()) {
            case 'A':
            case 'B':
                ac.modifiers = Modifier.PRIVATE;
                break;
            case 'C':
            case 'D':
                ac.modifiers = Modifier.PRIVATE | Modifier.STATIC;
                break;
            case 'E':
            case 'F':
                ac.modifiers = Modifier.PRIVATE;
                ac.isVirtual = true;
                break;
            case 'G':
            case 'H':
                ac.modifiers = Modifier.PRIVATE;
                ac.isThunk = true;
                break;
            case 'I':
            case 'J':
                ac.modifiers = Modifier.PROTECTED;
                break;
            case 'K':
            case 'L':
                ac.modifiers = Modifier.PROTECTED | Modifier.STATIC;
                break;
            case 'M':
            case 'N':
                ac.modifiers = Modifier.PROTECTED;
                ac.isVirtual = true;
                break;
            case 'O':
            case 'P':
                ac.modifiers = Modifier.PROTECTED;
                ac.isThunk = true;
                break;
            case 'Q':
            case 'R':
                ac.modifiers = Modifier.PUBLIC;
                break;
            case 'S':
            case 'T':
                ac.modifiers = Modifier.PUBLIC | Modifier.STATIC;
                break;
            case 'U':
            case 'V':
                ac.modifiers = Modifier.PUBLIC;
                ac.isVirtual = true;
                break;
            case 'W':
            case 'X':
                ac.modifiers = Modifier.PUBLIC;
                ac.isThunk = true;
                break;
            case 'Y':
            case 'Z':
                // No modifier, no storage class
                ac.modifiers = 0;
                break;
            default:
                throw error("Unknown access level + storage class");
        }
        return ac;
    }

    private ClassRef parseTemplateType() throws DemanglingException {
        //System.out.println("# START OF parseTemplateParams()");
        String name = parseNameFragment();
        //return withEmptyQualifiedNames(new DemanglingOp<ClassRef>() { public ClassRef run() throws DemanglingException {
        List<TemplateArg> args = parseTemplateParams();
        
        List<Object> names = new ArrayList<Object>();
        parseNameQualifications(names);
		
        //String ns = parseNameFragment();
        
        //System.out.println("parseTemplateParams() = " + args + ", ns = " + names);
        ClassRef tr = new ClassRef(new Ident(name, args.toArray(new TemplateArg[args.size()])));
        tr.setEnclosingType(reverseNamespace(names));

        return tr;
		//}});
    }

    private void parseFunctionProperty(MemberRef mr) throws DemanglingException {
        mr.callingConvention = parseCallingConvention();
        TypeRef returnType = consumeCharIf('@') ? classType(void.class) : parseType(true);
        allQualifiedNames.clear();
        //withEmptyQualifiedNames(new DemanglingRunnable() { public void run() throws DemanglingException {
        List<TypeRef> paramTypes = parseParams();
        mr.paramTypes = paramTypes.toArray(new TypeRef[paramTypes.size()]);
        if (!consumeCharIf('Z')) {
            List<TypeRef> throwTypes = parseParams();
            mr.throwTypes = throwTypes.toArray(new TypeRef[throwTypes.size()]);
        }

        mr.setValueType(returnType);
		//}});
    }

    static class AnonymousTemplateArg implements TemplateArg {
        public AnonymousTemplateArg(String v) {
            this.v = v;
        }
        String v;

        //@Override
        public boolean matchesParam(Object param, Annotations annotations) {
        		return true; // TODO wtf ?
        }
        @Override
        public String toString() {
            return v;
        }

    }

    private TemplateArg parseTemplateParameter() throws DemanglingException {
        switch (peekChar()) {
            case '?':
            	consumeChar();
                return new AnonymousTemplateArg("'anonymous template param " + parseNumber(false) + "'");
            case '$':
            	consumeChar();
                switch (consumeChar()) {
                    case '0':
                        return new Constant(parseNumber(true));
                    case '2':
                        int a = parseNumber(true);
                        int b = parseNumber(true);
                        return new Constant(a * Math.exp(10 * (int)Math.log(b - Math.log10(a) + 1)));
                    case 'D':
                        return new AnonymousTemplateArg("'anonymous template param " + parseNumber(false) + "'");
                    case 'F':
                        return new AnonymousTemplateArg("'tuple (" + parseNumber(true) + ", " + parseNumber(true) + ")'");
                    case 'G':
                        return new AnonymousTemplateArg("'tuple (" + parseNumber(true) + ", " + parseNumber(true) + ", " + parseNumber(true) + ")'");
                    case 'Q':
                        return new AnonymousTemplateArg("'anonymous non-type template param " + parseNumber(false) + "'");
                }
                break;
            default:
            	//error("Unexpected template param value");
        }
        return parseType(true);
    }
    static class AccessLevelAndStorageClass {
        int modifiers;
        boolean isVirtual = false, isThunk = false;
            
    }
	public MemberRef parseSymbol() throws DemanglingException {
		MemberRef mr = new MemberRef();

        int iAt = str.indexOf('@');
        if (iAt >= 0 && consumeCharIf('_')) {
            if (iAt > 0) {
                mr.setMemberName(new Ident(str.substring(1, iAt)));
                mr.setArgumentsStackSize(Integer.parseInt(str.substring(iAt + 1)));
                return mr;
            }
        }
		if (consumeCharIf('?')) {
            consumeCharsIf('@', '?');

            IdentLike memberName = parseFirstQualifiedTypeNameComponent();
            if (memberName instanceof SpecialName) {
                SpecialName specialName = (SpecialName)memberName;
                if (!specialName.isFunction())
                    return null;
            }
            mr.setMemberName(memberName);
            List<Object> qNames = new ArrayList<Object>();
            parseNameQualifications(qNames);

			//TypeRef qualifiedName = parseQualifiedTypeName();

            AccessLevelAndStorageClass ac = parseAccessLevelAndStorageClass();
            CVClassModifier cvMod = null;
            if (ac.modifiers != 0 && !Modifier.isStatic(ac.modifiers))
                cvMod = parseCVClassModifier();

            // Function property :
            allQualifiedNames.clear(); // TODO fix this !!
            parseFunctionProperty(mr);
            if (cvMod != null && (cvMod.isMember || (memberName instanceof SpecialName) || Modifier.isPublic(ac.modifiers))) {
            	Object r = qNames.get(0);
				ClassRef tr = r instanceof ClassRef ? (ClassRef)r : new ClassRef(new Ident((String)r));
                //tr.setSimpleName(qNames.get(0));
                qNames.remove(0);
                tr.setEnclosingType(reverseNamespace(qNames));
                mr.setEnclosingType(tr);
            } else {
                mr.setEnclosingType(reverseNamespace(qNames));
            }
            if (position != length)
                error("Failed to demangle the whole symbol");
		} else {
            mr.setMemberName(new Ident(str));
        }
		return mr;
	}


    TypeRef parseReturnType() throws DemanglingException {
        TypeRef tr = parseType(true);
        return tr;
    }
    int parseNumber(boolean allowSign) throws DemanglingException {
        int sign = allowSign && consumeCharIf('?') ? -1 : 1;
        if (Character.isDigit(peekChar())) {
            char c = consumeChar();
            return sign * (int)(c - '0');
        }
        if (peekChar() == '@')
            return 0;

        char c;
        StringBuilder b = new StringBuilder();
        long n = 0;
        while (((c = consumeChar()) >= 'A' && c <= 'P') && c != '@')
            n += 16 * (c - 'A');
        
        String s = b.toString().trim();
        if (c != '@' || s.length() == 0)
            throw error("Expected a number here", -b.length());
        return sign * Integer.parseInt(s, 16);
    }
	TypeRef parseType(boolean allowVoid) throws DemanglingException {
        char c = consumeChar();
        if (Character.isDigit(c)) {
            int iBack = (int)(c - '0');
            return getBackRef(iBack);
        }
		switch (c) {
		case '_':
			TypeRef tr;
			switch (consumeChar()) {
            case 'D': // __int8
            case 'E': // unsigned __int8
                tr = classType(byte.class);
				break;
            case 'F': // __int16
            case 'G': // unsigned __int16
                tr = classType(short.class);
				break;
            case 'H': // __int32
            case 'I': // unsigned __int32
                tr = classType(int.class);
				break;
            case 'J': // __int64
            case 'K': // unsigned __int64
                tr = classType(long.class);
				break;
            case 'L': // __int128
                tr = classType(BigInteger.class);
				break;
			case 'N': // bool
                tr = classType(boolean.class);
				break;
			case '0': // array ??
                parseCVClassModifier();
                parseType(false);
                tr = classType(Object[].class);
				break;
			case 'W':
				tr = classType(char.class);//, Wide.class);
				break;
			default:
				throw error(-1);
			}
			allQualifiedNames.add(tr);
			return tr;
        case 'Z':
            return classType(Object[].class);
        case 'O':
            throw error("'long double' type cannot be mapped !", -1);
		case 'C': // signed char
		case 'D': // char
		case 'E': // unsigned char
			return classType(byte.class);
		case 'F': // short
		case 'G': // unsigned short
			return classType(short.class);
		case 'H': // int
		case 'I': // unsigned int
			return classType(int.class);
		case 'J': // long
		case 'K': // unsigned long
			return classType(CLong.class);
        case 'M': // float
            return classType(float.class);
		case 'N': // double
			return classType(double.class);
        case 'Y':
            throw error("TODO handle cointerfaces", -1);
		case 'X':
            // TODO handle coclass case
            if (!allowVoid)
                return null;
			return classType(void.class);
        case '?':
            parseCVClassModifier(); // TODO do something with this !
            return parseType(allowVoid);
        case 'A': // reference
        case 'B': // volatile reference
        case 'P': // pointer
        case 'Q': // const pointer
        case 'R': // volatile pointer
        case 'S': // const volatile pointer
            if (!consumeCharsIf('$', 'A')) // __gc
                consumeCharsIf('$', 'B');  // __pin

            CVClassModifier cvMods = parseCVClassModifier();
            if (cvMods.isVariable) {
                if (consumeCharIf('Y')) {
                    int dimensions = parseNumber(false);
                    int[] indices = new int[dimensions];
                    for (int i = 0; i < dimensions; i++)
                        indices[i] = parseNumber(false);
                }
                tr = pointerType(parseType(true));
            } else {
                MemberRef mr = new MemberRef();
                parseFunctionProperty(mr);
                tr = pointerType(new FunctionTypeRef(mr));
            }
            allQualifiedNames.add(tr);
            return tr;
        case 'V': // class
        case 'U': // struct
        case 'T': // union
			//System.out.println("Found struct, class or union");
            return parseQualifiedTypeName();
        case 'W': // enum
            Class<?> cl;
            switch (consumeChar()) {
                case '0':
                case '1':
                    cl = byte.class;
                    break;
                case '2':
                case '3':
                    cl = short.class;
                    break;
                case '4':
                case '5':
                    cl = int.class;
                    break;
                case '6':
                case '7': // CLong : int on win32 and win64 !
                    cl = int.class;
                    break;
                default:
                    throw error("Unfinished enum", -1);
            }
            TypeRef qn = parseQualifiedTypeName();
            allQualifiedNames.add(qn);
            return classType(cl);
		default:
			throw error(-1);
		}
	}
    static NamespaceRef reverseNamespace(List<Object> names) {
        if (names == null || names.isEmpty())
            return null;
        Collections.reverse(names);
        return new NamespaceRef(names.toArray(new Object[names.size()]));
    }
    List<TypeRef> allQualifiedNames = new ArrayList<TypeRef>();
    interface DemanglingOp<T> {
    	T run() throws DemanglingException;
    }
	<T> T withEmptyQualifiedNames(DemanglingOp<T> action) throws DemanglingException {
		List<TypeRef> list = allQualifiedNames;
    	try {
    		allQualifiedNames = new ArrayList<TypeRef>();
    		return action.run();
    	} finally {
    		allQualifiedNames = list;
    	}
    }

    IdentLike parseFirstQualifiedTypeNameComponent() throws DemanglingException {
        if (consumeCharIf('?')) {
            if (consumeCharIf('$'))
                return parseTemplateType().getIdent();
            else
                return parseSpecialName();
        }
        else
            return new Ident(parseNameFragment());
    }
    TypeRef parseQualifiedTypeName() throws DemanglingException {
        char c = peekChar();
    	List<Object> names = new ArrayList<Object>();
        names.add(parseFirstQualifiedTypeNameComponent());
        parseNameQualifications(names);
		
		if (names.size() == 1 && (names.get(0) instanceof TypeRef)) {
			return (TypeRef)names.get(0);
		}
		
        /*
    	if (Character.isDigit(c)) {
    		consumeChar();
    		int i = (int)(c - '0');
    		if (i < 0 || i >= allQualifiedNames.size())
    			throw error("Invalid back reference " + i + " (knows only " + allQualifiedNames + ")", -1);
    		names = new ArrayList<String>(allQualifiedNames.get(i));
    	} else {
    		names = parseNames();
    	}*/

        ClassRef tr = new ClassRef((Ident)names.get(0));
        names.remove(0);
        tr.setEnclosingType(reverseNamespace(names));
        return tr;
    }

    public IdentLike parseSpecialName() throws DemanglingException {
        switch (consumeChar()) {
        case '0':
            return SpecialName.Constructor;
        case '1':
            return SpecialName.Destructor;
        case '2':
            return SpecialName.New;
        case '3':
            return SpecialName.Delete;
        case '4':
            return SpecialName.OperatorAssign;
        case '5':
            return SpecialName.OperatorRShift;
        case '6':
            return SpecialName.OperatorLShift;
        case '7':
            return SpecialName.OperatorLogicNot;
        case '8':
            return SpecialName.OperatorEquals;
        case '9':
            return SpecialName.OperatorDifferent;
        case 'A':
            return SpecialName.OperatorSquareBrackets;
        case 'B':
            return SpecialName.OperatorCast;
        case 'C':
            return SpecialName.OperatorArrow;
        case 'D':
            return SpecialName.OperatorMultiply;
        case 'E':
            return SpecialName.OperatorIncrement;
        case 'F':
            return SpecialName.OperatorDecrement;
        case 'G':
            return SpecialName.OperatorSubstract;
        case 'H':
            return SpecialName.OperatorAdd;
        case 'I':
            return SpecialName.OperatorBitAnd;
        case 'J':
            return SpecialName.OperatorArrowStar;
        case 'K':
            return SpecialName.OperatorDivide;
        case 'L':
            return SpecialName.OperatorModulo;
        case 'M':
            return SpecialName.OperatorLower;
        case 'N':
            return SpecialName.OperatorLowerEquals;
        case 'O':
            return SpecialName.OperatorGreater;
        case 'P':
            return SpecialName.OperatorGreaterEquals;
        case 'Q':
            return SpecialName.OperatorComma;
        case 'R':
            return SpecialName.OperatorParenthesis;
        case 'S':
            return SpecialName.OperatorBitNot;
        case 'T':
            return SpecialName.OperatorXOR;
        case 'U':
            return SpecialName.OperatorBitOr;
        case 'V':
            return SpecialName.OperatorLogicAnd;
        case 'W':
            return SpecialName.OperatorLogicOr;
        case 'X':
            return SpecialName.OperatorMultiplyAssign;
        case 'Y':
            return SpecialName.OperatorAddAssign;
        case 'Z':
            return SpecialName.OperatorSubstractAssign;
        case '_':
            switch (consumeChar()) {
                case '0':
                    return SpecialName.OperatorDivideAssign;
                case '1':
                    return SpecialName.OperatorModuloAssign;
                case '2':
                    return SpecialName.OperatorLShiftAssign;
                case '3':
                    return SpecialName.OperatorRShiftAssign;
                case '4':
                    return SpecialName.OperatorBitAndAssign;
                case '5':
                    return SpecialName.OperatorBitOrAssign;
                case '6':
                    return SpecialName.OperatorXORAssign;
                case '7':
                    return SpecialName.VFTable;
                case '8':
                    return SpecialName.VBTable;
                case '9':
                    return SpecialName.VCall;
                case 'E':
                    return SpecialName.VectorDeletingDestructor;
                case 'G':
                    return SpecialName.ScalarDeletingDestructor;
                default:
                    throw error("unhandled extended special name");
            }
            
        default:
            throw error("Invalid special name");
        }
    }

    private List<TypeRef> parseParams() throws DemanglingException {
        List<TypeRef> paramTypes = new ArrayList<TypeRef>();
        if (!consumeCharIf('X')) {
            char c;
			while ((c = peekChar()) != '@' && c != 0 && (c != 'Z' || peekChar(2) == 'Z')) {
                TypeRef tr = parseType(false);
                if (tr == null)
                    continue;
                paramTypes.add(tr);
            }
            if (c == 'Z')
                consumeChar();
                //break;
            if (c == '@')
                consumeChar();
        }
        return paramTypes;
    }
    private List<TemplateArg> parseTemplateParams() throws DemanglingException {
        return withEmptyQualifiedNames(new DemanglingOp<List<TemplateArg>>() { public List<TemplateArg> run() throws DemanglingException {
            List<TemplateArg> paramTypes = new ArrayList<TemplateArg>();
            if (!consumeCharIf('X')) {
                char c;
                while ((c = peekChar()) != '@' && c != 0) {
                    TemplateArg tr = parseTemplateParameter();
                    if (tr == null)
                        continue;
                    paramTypes.add(tr);
                }
            }
            return paramTypes;
        }});
    }

    String parseNameFragment() {
		StringBuilder b = new StringBuilder();
		char c;

		while ((c = consumeChar()) != '@')
			b.append(c);

		String name = b.toString();
		//allQualifiedNames.add(Collections.singletonList(name));
		return name;
	}

    TypeRef getBackRef(int i) throws DemanglingException {
        if (i == allQualifiedNames.size())
            i--; // TODO fix this !!!

        if (i < 0 || i >= allQualifiedNames.size())
            throw error("Invalid back references in name qualifications", -1);
        return allQualifiedNames.get(i);
    }
    private void parseNameQualifications(List<Object> names) throws DemanglingException {
        if (Character.isDigit(peekChar())) {
            try {
                int i = consumeChar() - '0';
                names.add(getBackRef(i));
                expectChars('@');
                return;
            } catch (Exception ex) {
                throw error("Invalid back references in name qualifications", -1);
            }
        }

        while (peekChar() != '@')
            names.add(parseNameQualification());

        expectChars('@');
    }
    Object parseNameQualification() throws DemanglingException {
        if (consumeCharIf('?')) {
            if (consumeCharIf('$'))
                return parseTemplateType();
            else {
                if (peekChar() == 'A')
                    throw error("Anonymous numbered namespaces not handled yet");
                int namespaceNumber = parseNumber(false);
                return String.valueOf(namespaceNumber);
            }
        } else
            return parseNameFragment();
    }

    Style parseCallingConvention() throws DemanglingException {
        Convention.Style cc;
        boolean exported = true;
        switch (consumeChar()) {
            case 'A':
                exported = false;
            case 'B':
                cc = Convention.Style.CDecl;
                break;
            case 'C':
                exported = false;
            case 'D':
                cc = Convention.Style.Pascal;
                break;
            case 'E':
                exported = false;
            case 'F':
                cc = Convention.Style.ThisCall;
                break;
            case 'G':
                exported = false;
            case 'H':
                cc = Convention.Style.StdCall;
                break;
            case 'I':
                exported = false;
            case 'J':
                cc = Convention.Style.FastCall;
                break;
            case 'K':
                exported = false;
            case 'L':
                cc = null;
                break;
            case 'N':
                cc = Convention.Style.CLRCall;
                break;
            default:
                throw error("Unknown calling convention");
        }
        return cc;
    }
    static class CVClassModifier {
        boolean isVariable;
        boolean isMember;
        boolean isBased;
    }
    CVClassModifier parseCVClassModifier() throws DemanglingException {
        CVClassModifier mod = new CVClassModifier();
        switch (peekChar()) {
            case 'E': // __ptr64
            case 'F': // __unaligned 
            case 'I': // __restrict
                consumeChar();
                break;
        }
        boolean based = false;
        switch (consumeChar()) {
            case 'M': // __based
            case 'N': // __based
            case 'O': // __based
            case 'P': // __based
                mod.isBased = true;
            case 'A':
            case 'B':
            case 'J':
            case 'C':
            case 'G':
            case 'K':
            case 'D':
            case 'H':
            case 'L':
                mod.isVariable = true;
                mod.isMember = false;
                break;
            case '2': // __based
            case '3': // __based
            case '4': // __based
            case '5': // __based
                mod.isBased = true;
            case 'Q':
            case 'U':
            case 'Y':
            case 'R':
            case 'V':
            case 'Z':
            case 'S':
            case 'W':
            case '0':
            case 'T':
            case 'X':
            case '1':
                mod.isVariable = true;
                mod.isMember = true;
                break;
            case '_': // __based
                mod.isBased = true;
                switch (consumeChar()) {
                    case 'A':
                    case 'B':
                        mod.isVariable = false;
                        break;
                    case 'C':
                    case 'D':
                        mod.isVariable = false;
                        mod.isMember = true;
                        break;
                    default:
                        throw error("Unknown extended __based class modifier", -1);
                }
                break;
            case '6':
            case '7':
                mod.isVariable = false;
                mod.isMember = false;
                break;
            case '8':
            case '9':
                mod.isVariable = false;
                mod.isMember = true;
                break;
            default:
                throw error("Unknown CV class modifier", -1);
        }
        if (mod.isBased) {
            switch (consumeChar()) {
                case '0': // __based(void)
                    break;
                case '2':
                    parseNameQualifications(new ArrayList<Object>());
                    break;
                case '5': // no __based() ??
                    break;
            }
        }
        return mod;
    }
}