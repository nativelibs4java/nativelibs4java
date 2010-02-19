package com.bridj;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.bridj.ann.CLong;
import com.bridj.ann.Wide;

public class VC9Demangler extends Demangler {
	public VC9Demangler(String str) {
		super(str);
	}
	public MemberRef parseSymbol() throws DemanglingException {
		MemberRef mr = new MemberRef();
		
		if (consumeCharIf('?')) {
			boolean isFunctionOrMethod = str.endsWith("Z");
			boolean isMemberFunction = false;
			if (consumeCharIf('?')) {
				switch (consumeChar()) {
				case '1': // "??1"
					mr.type = MemberRef.Type.Destructor;
					break;
				case '_': // "??1"
					mr.type = MemberRef.Type.ScalarDeletingDestructor;
					break;
				default:
					throw error(-1);
				}
			}
			
			mr.memberName = parseName();
			List<String> ns = parseNames();
			
			if (isFunctionOrMethod) {
				char type = consumeChar();
				switch (type) {
				case 'Y':
					mr.type = MemberRef.Type.CFunction;
					break;
				case 'Q':
				case 'U': // WTF ??
					mr.modifiers = Modifier.PUBLIC;
					break;
				case 'A':
					mr.modifiers = Modifier.PRIVATE;
					break;
				case 'C':
					mr.modifiers = Modifier.PRIVATE | Modifier.STATIC;
					break;
				case 'I':
					mr.modifiers = Modifier.PROTECTED;
					break;
				case 'K':
					mr.modifiers = Modifier.PROTECTED | Modifier.STATIC;
					break;
				case 'S':
					mr.modifiers = Modifier.PUBLIC | Modifier.STATIC;
					break;
				default:
					throw error(-1);
				}
				if (mr.type == null)
					mr.type = MemberRef.Type.Method;
					
				if (mr.type != MemberRef.Type.CFunction) {
					//ns.
				}
				
				if (consumeChar() != 'A')
					throw error(-1);
				
				mr.valueType = parseType(true);
				List<TypeRef> paramTypes = new ArrayList<TypeRef>();
				char c;
				while ((c = peekChar()) != '@' && c != 'Z' && c != 0) {
                    TypeRef tr = parseType(false);
                    if (tr == null)
                        continue;
					paramTypes.add(tr);
                }


				mr.paramTypes = paramTypes.toArray(new TypeRef[paramTypes.size()]);
			}
			
			mr.enclosingType = reverseNamespace(ns);
		}
		return mr;
	}
	TypeRef parseType(boolean allowVoid) throws DemanglingException {
		switch (consumeChar()) {
		case '_':
			switch (consumeChar()) {
			case 'J':
			case 'K': // unsigned
				return classType(Long.TYPE);
			case 'W':
				return classType(Character.TYPE, Wide.class);
			default:
				throw error(-1);
			}
		case 'D':
		case 'E': // unsigned
		case 'C': // signed
			return classType(Byte.TYPE);
		case 'J':
		case 'K': // unsigned
			return classType(Long.TYPE, CLong.class);
		case 'H':
		case 'I': // unsigned
			return classType(Integer.TYPE);
		case 'F':
			return classType(Short.TYPE);
		case 'X':
            if (!allowVoid)
                return null;
			return classType(Void.TYPE);
		case 'M':
			return classType(Float.TYPE);
		case 'N':
			return classType(Double.TYPE);
		case 'P':
        //case 'A': // reference ?
            if (consumeChar() == 'E') //'E'
				consumeChar(); //'A'
				
			parseType(allowVoid); // TODO use it
			return classType(Pointer.class);
        case 'V': // class
        case 'U': // struct
        case 'T': // union
			//System.out.println("Found struct, class or union");
            return parseQualifiedTypeName();
		default:
			throw error(-1);
		}
	}
    static NamespaceRef reverseNamespace(List<String> names) {
        if (names == null || names.isEmpty())
            return null;
        Collections.reverse(names);
        return new NamespaceRef(names.toArray(new String[names.size()]));
    }
    TypeRef parseQualifiedTypeName() {
        List<String> names = parseNames();

        ClassRef tr = new ClassRef();
        tr.simpleName = names.get(0);
        names.remove(0);
        tr.enclosingType = reverseNamespace(names);
        return tr;
    }
	List<String> parseNames() {
		List<String> ns = new ArrayList<String>();
		while (peekChar() != '@')
			ns.add(parseName());

		consumeChar();
		return ns;
	}		
	String parseName() {
		StringBuilder b = new StringBuilder();
		char c;
		
		while ((c = consumeChar()) != '@')
			b.append(c);
		
		return b.toString();
	}
}