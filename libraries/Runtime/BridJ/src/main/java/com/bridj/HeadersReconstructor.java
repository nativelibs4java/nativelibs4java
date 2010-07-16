package com.bridj;
import java.util.*;
import java.io.*;
import static com.bridj.Demangler.*;
public class HeadersReconstructor {
	
	public static void reconstructHeaders(Iterable<NativeLibrary> libraries, PrintWriter out) {
		List<MemberRef> orphanMembers = new ArrayList<MemberRef>();
		Map<TypeRef, List<MemberRef>> membersByClass = new HashMap<TypeRef, List<MemberRef>>();
		for (NativeLibrary library : libraries) {
			for (Symbol symbol : library.getSymbols()) {
				MemberRef mr = symbol.getParsedRef();
				if (mr == null)
					continue;
				
				TypeRef et = mr.getEnclosingType();
				if (et == null)
					orphanMembers.add(mr);
				else {
					List<MemberRef> mrs = membersByClass.get(et);
					if (mrs == null)
						membersByClass.put(et, mrs = new ArrayList<MemberRef>());
					mrs.add(mr);
				}
			}
		}
		for (TypeRef tr : membersByClass.keySet())
			out.println("class " + tr + ";");
		
		for (MemberRef mr : orphanMembers)
			out.println(mr + ";");
		
		for (Map.Entry<TypeRef, List<MemberRef>> e : membersByClass.entrySet()) {
			TypeRef tr = e.getKey();
			List<MemberRef> mrs = e.getValue();
			out.println("class " + tr + " \n{");
			for (MemberRef mr : mrs) {
				out.println("\t" + mr + ";");
			}
			out.println("}");
		}
		 
	}
	
}
