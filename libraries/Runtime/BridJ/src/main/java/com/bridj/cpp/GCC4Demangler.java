package com.bridj.cpp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bridj.Demangler;
import com.bridj.JNI;
import com.bridj.NativeLibrary;
import com.bridj.Pointer;
import com.bridj.Demangler.ClassRef;
import com.bridj.Demangler.DemanglingException;
import com.bridj.Demangler.MemberRef;
import com.bridj.Demangler.NamespaceRef;
import com.bridj.Demangler.TypeRef;
import com.bridj.Demangler.SpecialName;

public class GCC4Demangler extends Demangler {
	
	public GCC4Demangler(NativeLibrary library, String symbol) {
		super(library, symbol);
	}
	
	public TypeRef parseType() throws DemanglingException {
		if (Character.isDigit(peekChar()))
			return simpleType(parseName());
		
		switch (consumeChar()) {
		case 'P':
			parseType(); // TODO we don't care what this points to right now
			return classType(Pointer.class);
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
		MemberRef mr = new MemberRef();
		if (!consumeCharIf('_')) {
			mr.setMemberName(str);
			return mr;
		}
		consumeCharIf('_');
		expectChars('Z');
		
		if (peekChar() == 'T')
			return null; // can be a type info, a virtual table or strange things like that
		
		List<String> ns = new ArrayList<String>();
		if (consumeCharIf('N')) {
			do {
				ns.add(parseName());
			} while (Character.isDigit(peekChar()));
			mr.setMemberName(ns.remove(ns.size() - 1));
			if (!ns.isEmpty()) {
				ClassRef parent = new ClassRef();
				parent.setSimpleName(ns.remove(ns.size() - 1));
				if (!ns.isEmpty())
					parent.setEnclosingType(new NamespaceRef(ns.toArray(new String[ns.size()])));
				mr.setEnclosingType(parent);
			} else {
				switch (peekChar()) {
				case 'C':
					consumeChar();
					expectAnyChar('1', '2');
					mr.setEnclosingType(new ClassRef((String)mr.getMemberName()));
					mr.setMemberName(SpecialName.Constructor);
					break;
				case 'D':
					consumeChar();
					expectAnyChar('1', '2');
					mr.setEnclosingType(new ClassRef((String)mr.getMemberName()));
					mr.setMemberName(SpecialName.Destructor);
					break;
				}
			}
		} else {
			//mr.type = SpecialName.CFunction;
			mr.setMemberName(parseName());
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
