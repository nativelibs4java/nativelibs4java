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
					throw error();
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
					throw error();
				}
				if (mr.type == null)
					mr.type = MemberRef.Type.Method;
					
				if (mr.type != MemberRef.Type.CFunction) {
					//ns.
				}
				
				if (consumeChar() != 'A')
					throw error();
				
				mr.valueType = parseType();
				List<TypeRef> paramTypes = new ArrayList<TypeRef>();
				char c;
				while ((c = peekChar()) != '@' && c != 'Z' && c != 0)
					paramTypes.add(parseType());
				
				mr.paramTypes = paramTypes.toArray(new TypeRef[paramTypes.size()]);
			}
			
			Collections.reverse(ns);
			
			NamespaceRef parent = new NamespaceRef();
			parent.namespace = ns.toArray(new String[ns.size()]);
			mr.enclosingType = parent;
		}
		return mr;
	}
	TypeRef parseType() throws DemanglingException {
		switch (consumeChar()) {
		case '_':
			switch (consumeChar()) {
			case 'J':
			case 'K': // unsigned
				return classType(Long.TYPE);
			case 'W':
				return classType(Character.TYPE, Wide.class);
			default:
				throw error();
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
			return classType(Void.TYPE);
		case 'M':
			return classType(Float.TYPE);
		case 'N':
			return classType(Double.TYPE);
		case 'P':
			parseType(); // TODO use it
			return classType(Pointer.class);
		default:
			throw error();
		}
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