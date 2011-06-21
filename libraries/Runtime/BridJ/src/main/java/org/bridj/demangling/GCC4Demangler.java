package org.bridj.demangling;

import java.util.ArrayList;
import java.util.List;

import org.bridj.demangling.Demangler;
import org.bridj.JNI;
import org.bridj.CLong;
import org.bridj.NativeLibrary;
import org.bridj.demangling.Demangler.ClassRef;
import org.bridj.demangling.Demangler.DemanglingException;
import org.bridj.demangling.Demangler.Ident;
import org.bridj.demangling.Demangler.MemberRef;
import org.bridj.demangling.Demangler.NamespaceRef;
import org.bridj.demangling.Demangler.TypeRef;
import org.bridj.demangling.Demangler.SpecialName;
import java.util.HashMap;
import java.util.Map;
import org.bridj.Platform;

public class GCC4Demangler extends Demangler {
	
	public GCC4Demangler(NativeLibrary library, String symbol) {
		super(library, symbol);
	}

    private Map<String, TypeRef> shortcuts = new HashMap<String, TypeRef>();
    int nextShortcutId = -1;
    private String nextShortcutId() {
        int n = nextShortcutId++;
        return n == -1 ? "_" : Integer.toString(n, 36).toUpperCase() + "_";
    }

    private TypeRef parseShortcutType() {
        if (peekChar() == '_') {
            return shortcuts.get(Character.toString(consumeChar()));
        }
        String id = "";
        char c;
        while ((c = peekChar()) != '_' && c != 'E' && c != 0) {
            id += consumeChar();
        }
        if (c != 'E')
            id += consumeChar();

        if (id.equals("s"))
            return classType(StdString.class);

        TypeRef res = shortcuts.get(id);
        return res;
    }
    private TypeRef parsePointerType() throws DemanglingException {
        TypeRef pointed = parseType();
        TypeRef res = pointerType(pointed);
        String id = nextShortcutId();
        shortcuts.put(id, res);
        return res;
    }

    public TemplateArg parseTemplateArg() throws DemanglingException {
    		if (consumeCharIf('L')) {
    			TypeRef tr = parseType();
    			StringBuffer b = new StringBuffer();
    			char c;
                while (Character.isDigit(c = peekChar())) {
                    consumeChar();
                    b.append(c);
                }
                expectChars('E');
			// TODO switch on type !
			return new Constant(Integer.parseInt(b.toString()));
    		} else
    			return parseType();
    }
	public TypeRef parseType() throws DemanglingException {
		if (Character.isDigit(peekChar())) {
                    Ident name = parseIdent();
                    String id = nextShortcutId();
                    TypeRef res = simpleType(name);
                    shortcuts.put(id, res);
                    return res;
                }

        char c = consumeChar();
		switch (c) {
		case 'S':
			return parseShortcutType();
		case 'P':
			return parsePointerType();
		case 'F':
			// TODO parse function type correctly !!!
			while (consumeChar() != 'E') {}
			
			return null;
		case 'K':
			return parseType();
		case 'v': // char
			return classType(Void.TYPE);
		case 'c':
		case 'a':
		case 'h': // unsigned
			return classType(Byte.TYPE);
		case 'b': // bool
			return classType(Boolean.TYPE);
		case 'l':
		case 'm': // unsigned
			return classType(CLong.class);
			//return classType(Platform.is64Bits() ? Long.TYPE : Integer.TYPE);
		case 'x':
		case 'y': // unsigned
			return classType(Long.TYPE);
		case 'i':
		case 'j': // unsigned
			return classType(Integer.TYPE);
		case 's':
		case 't': // unsigned
			return classType(Short.TYPE);
		case 'f':
			return classType(Float.TYPE);
		case 'd':
			return classType(Double.TYPE);
		case 'z': // varargs
			return classType(Object[].class);
        default:
			throw error("Unexpected type char '" + c + "'", -1);
		}
	}
    public static class StdString {}

	String parseName() throws DemanglingException {
		StringBuilder b = new StringBuilder();
		char c;
		while (Character.isDigit(c = peekChar())) {
			consumeChar();
			b.append(c);
		}
		int len;
		try {
			len = Integer.parseInt(b.toString());
		} catch (NumberFormatException ex) {
			throw error("Expected a number", 0);
		}
		b.setLength(0);
		for (int i = 0; i < len; i++)
			b.append(consumeChar());
		return b.toString();
	}
    Ident parseIdent() throws DemanglingException {
        String n = parseName();
        List<TemplateArg> args = new ArrayList<TemplateArg>();
        if (consumeCharIf('I')) {
            while (!consumeCharIf('E'))
                args.add(parseTemplateArg());
        }
        return new Ident(n, args.toArray(new TemplateArg[args.size()]));
    }
	@Override
	public MemberRef parseSymbol() throws DemanglingException {
		MemberRef mr = new MemberRef();
		if (!consumeCharIf('_')) {
			mr.setMemberName(new Ident(str));
			return mr;
		}
		consumeCharIf('_');
		expectChars('Z');
		
		if (consumeCharIf('T')) {
			if (consumeCharIf('V')) {
				mr.setEnclosingType(new ClassRef(parseIdent()));
				mr.setMemberName(SpecialName.VFTable);
				return mr;
			}
			return null; // can be a type info, a virtual table or strange things like that
		}
		/*
			Reverse engineering of C++ operators :
			delete[] = __ZdaPv
			delete  = __ZdlPv
			new[] = __Znam
			new = __Znwm
		*/
		if (consumeCharsIf('d', 'l', 'P', 'v')) {
			mr.setMemberName(SpecialName.Delete);
			return mr;
		}
		if (consumeCharsIf('d', 'a', 'P', 'v')) {
			mr.setMemberName(SpecialName.DeleteArray);
			return mr;
		}
		if (consumeCharsIf('n', 'w', 'm')) {
			mr.setMemberName(SpecialName.New);
			return mr;
		}
		if (consumeCharsIf('n', 'a', 'm')) {
			mr.setMemberName(SpecialName.NewArray);
			return mr;
		}
		
		List<Ident> ns = new ArrayList<Ident>();
		if (consumeCharIf('N')) {
			do {
                            // TODO better than simple increment
                            nextShortcutId++;
				ns.add(parseIdent());
			} while (Character.isDigit(peekChar()));
			nextShortcutId--; // correct the fact that we parsed one too much
			mr.setMemberName(ns.remove(ns.size() - 1));
			if (!ns.isEmpty()) {
				ClassRef parent = new ClassRef(ns.remove(ns.size() - 1));
				if (!ns.isEmpty())
					parent.setEnclosingType(new NamespaceRef(ns.toArray(new Ident[ns.size()])));
				mr.setEnclosingType(parent);
			} else {
                // for templates, for instance "_ZN24InvisibleSourcesTemplateILi10ESsEC1Ei"
                char c = peekChar();
				switch (c) {
				/*case 'I':
					List<TemplateArg> args = new ArrayList<TemplateArg>();
					consumeChar();
					while (!consumeCharIf('E')) {
						args.add(parseTemplateArg());
					}
					//System.out.println("HEHEHEHEHEHEHE args = " + args);
					mr.setTemplateArguments(args.toArray(new TemplateArg[args.size()]));
					break;*/
				case 'C':
					consumeChar();
					mr.setEnclosingType(new ClassRef((Ident)mr.getMemberName()));
					if (consumeCharIf('1'))
						mr.setMemberName(SpecialName.Constructor);
					else if (consumeCharIf('2'))
						mr.setMemberName(SpecialName.SpecialConstructor);
                    else
                        error("Unknown constructor type");
					break;
				case 'D':
					consumeChar();
					mr.setEnclosingType(new ClassRef((Ident)mr.getMemberName()));
                    // see http://zedcode.blogspot.com/2007/02/gcc-c-link-problems-on-small-embedded.html
					if (consumeCharIf('0'))
						mr.setMemberName(SpecialName.DeletingDestructor);
					else if (consumeCharIf('1'))
						mr.setMemberName(SpecialName.Destructor);
					else if (consumeCharIf('2'))
						mr.setMemberName(SpecialName.SelfishDestructor);
                    else
                        error("Unknown destructor type");
					break;
				}
			}
		} else {
			//mr.type = SpecialName.CFunction;
			mr.setMemberName(parseIdent());
		}
		//System.out.println("mr = " + mr + ", peekChar = " + peekChar());
					
		//mr.isStatic =
		boolean isMethod = consumeCharIf('E');
		
		if (consumeCharIf('v')) {
			if (position < length)
				error("Expected end of symbol", 0);
			mr.paramTypes = new TypeRef[0];
		} else {
			List<TypeRef> paramTypes = new ArrayList<TypeRef>();
			while (position < length) {// && !consumeCharIf('E')) {
				paramTypes.add(parseType());
			}
			mr.paramTypes = paramTypes.toArray(new TypeRef[paramTypes.size()]);
		}
		return mr;
	}

	
}
