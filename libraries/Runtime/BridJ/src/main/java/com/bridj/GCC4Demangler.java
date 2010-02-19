package com.bridj;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bridj.Demangler.DemanglingException;
import com.bridj.Demangler.MemberRef.Type;

class GCC4Demangler extends Demangler {
	
	public GCC4Demangler(NativeLibrary library, String symbol) {
		super(library, symbol);
	}
	
	public TypeRef parseType() throws DemanglingException {
		switch (consumeChar()) {
		case 'P':
			parseType(); // TODO we don't care what this points to right now
			return classType(Pointer.class);
		case 'K':
			return parseType();
		case 'v': // char
			return classType(Void.TYPE);
		case 'c':
		case 'h': // unsigned
		case 'b': // bool
			return classType(Byte.TYPE);
		case 'l':
		case 'm': // unsigned
			return classType(JNI.is64Bits() ? Long.TYPE : Integer.TYPE);
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
		default:
			throw error(-1);
		}
	}

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
	@Override
	public MemberRef parseSymbol() throws DemanglingException {
		expectChars('_', 'Z');
		
		if (peekChar() == 'T')
			return null; // can be a type info, a virtual table or strange things like that
		
		MemberRef mr = new MemberRef();
		List<String> ns = new ArrayList<String>();
		if (consumeCharIf('N')) {
			do {
				ns.add(parseName());
			} while (Character.isDigit(peekChar()));
			mr.memberName = ns.remove(ns.size() - 1);
			mr.type = Type.InstanceMethod;
			if (!ns.isEmpty()) {
				ClassRef parent = new ClassRef();
				parent.simpleName = ns.remove(ns.size() - 1);
				if (!ns.isEmpty())
					parent.enclosingType = new NamespaceRef(ns.toArray(new String[ns.size()]));
			} else {
				switch (peekChar()) {
				case 'C':
					consumeChar();
					expectAnyChar('1', '2');
					mr.type = Type.Constructor;
					break;
				case 'D':
					consumeChar();
					expectAnyChar('1', '2');
					mr.type = Type.Destructor;
					break;
				}
			}
		} else {
			mr.type = Type.CFunction;
			mr.memberName = parseName();
		}
		//mr.isStatic =
		boolean isMethod = consumeCharIf('E');
		
		if (consumeCharIf('v')) {
			if (position < length)
				error("Expected end of symbol", 0);
		} else {
			List<TypeRef> paramTypes = new ArrayList<TypeRef>();
			while (position < length) {
				paramTypes.add(parseType());
			}
			mr.paramTypes = paramTypes.toArray(new TypeRef[paramTypes.size()]);
		}
		return mr;
	}

	
}
