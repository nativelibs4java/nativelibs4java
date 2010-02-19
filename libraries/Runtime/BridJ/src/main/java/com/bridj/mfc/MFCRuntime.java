/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.bridj.mfc;

import com.bridj.CPPRuntime;
import com.bridj.Callback;
import com.bridj.Pointer;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Olivier
 */
public class MFCRuntime extends CPPRuntime {
    Method mfcGetMessageMap;
	String mfcGetMessageMapMangling;
	Callback mfcGetMessageMapCallback;

	Set<Class<?>> hasMessageMap = new HashSet<Class<?>>();

	
	public void getExtraFieldsOfNewClass(Class<?> type, Map<String, Type> out) {
		//super.getExtraFieldsOfNewClass(type, out);
		if (!hasMessageMap.contains(type))
			return;

		out.put("messageMap", Pointer.class);
	}
	
	public void getOverriddenVirtualMethods(Map<String, Pointer<?>> out) {
		//super.getVirtualMethodBindings(out);
		out.put("mfcGetMessageMap", Pointer.pointerTo(mfcGetMessageMapCallback, null));
	}

	
	public boolean register(Class<?> type) {
		//if (!super.register(type))
		//	return false;

		MessageMapBuilder map = new MessageMapBuilder();
		for (Method method : type.getMethods()) {

			OnCommand onCommand = method.getAnnotation(OnCommand.class);
			if (onCommand != null)
				map.add(method, onCommand);

			OnCommandEx onCommandEx = method.getAnnotation(OnCommandEx.class);
			if (onCommandEx != null)
				map.add(method, onCommandEx);

			OnUpdateCommand onUpdateCommand = method.getAnnotation(OnUpdateCommand.class);
			if (onUpdateCommand != null)
				map.add(method, onUpdateCommand);

			OnRegisteredMessage onRegisteredMessage = method.getAnnotation(OnRegisteredMessage.class);
			if (onRegisteredMessage != null)
				map.add(method, onRegisteredMessage);

			OnMessage onMessage = method.getAnnotation(OnMessage.class);
			if (onMessage != null)
				map.add(method, onMessage);
		}
		if (!map.isEmpty())
			map.register(this, type);

		return true;
	}
}
