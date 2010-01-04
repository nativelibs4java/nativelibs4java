/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.runtime;
import java.lang.annotation.Annotation;
import static com.nativelibs4java.runtime.JDyncallLibrary.*;
import static com.nativelibs4java.runtime.DyncallSignatures.*;
import com.nativelibs4java.runtime.ann.*;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;
/**
 *
 * @author Olivier
 */
public class MethodCallInfo {

    Method fMethod;
	Class<?> fReturnTypeClass;
	AddressableFactory<?> fAddressableReturnFactory;
	String fDCSignature;
	String fJavaSignature;
    List<ValueType> fArgTypes;
	List<Options> fArgOptions;
	Options fMethodOptions;
	ValueType fReturnType;
	boolean fIsVarArgs;
	boolean fIsStatic;
	boolean fIsCPlusPlus;

    OptimizationHints fOptimizationHints = new OptimizationHints();

    //DCCallback* fCallback;
	long fForwardedSymbol;
	int fDCMode;

	public MethodCallInfo(Method method, long forwardedSymbol) {
        fForwardedSymbol = forwardedSymbol;
        fIsVarArgs = false;
        fIsCPlusPlus = false;
        fDCMode = 0;
        fMethod = method;
        Class<?>[] paramsTypes = method.getParameterTypes();
        Annotation[][] paramsAnnotations = method.getParameterAnnotations();
        fReturnTypeClass = method.getReturnType();
        Type genericReturnType = method.getGenericReturnType();
        int modifiers = method.getModifiers();
        fIsStatic = Modifier.isStatic(modifiers);
        fIsVarArgs = method.isVarArgs();
        //bool isStatic = true; //TODO

        int nParams = paramsTypes.length;
        fArgTypes = new ArrayList<ValueType>(nParams);
        fArgOptions = new ArrayList<Options>(nParams);

        fOptimizationHints.isAdaptableAsRaw = nParams <= 4;
        fIsCPlusPlus = CPPObject.class.isAssignableFrom(method.getDeclaringClass());

        GetOptions(fMethodOptions, method);

        //TODO
        /*fDCMode = fIsCPlusPlus ?
            fIsStatic */

        //if (Addressable.class.isAssignableFrom(fReturnTypeClass))
        //    fAddressableReturnFactory = DynCall.newAddressableFactory((Class)fReturnTypeClass, method.getGenericReturnType());

        StringBuilder jsig = new StringBuilder(), dcsig = new StringBuilder();
        jsig.append('(');
        dcsig.append(DC_SIGCHAR_POINTER).append(DC_SIGCHAR_POINTER); // JNIEnv*, jobject: always present in native-bound functions

        for (int iParam = 0; iParam < nParams; iParam++) {
            Options paramOptions = new Options();
            fArgOptions.add(paramOptions);
            GetOptions(paramOptions, method, paramsAnnotations[iParam]);

            Class<?> param = paramsTypes[iParam];
            ValueType argType = GetJavaTypeSignature(param, jsig, dcsig, paramOptions, fOptimizationHints);
            fArgTypes.add(argType);

            //if (argType == e
        }
        jsig.append(')');
        dcsig.append(')');
        fReturnType = GetJavaTypeSignature(fReturnTypeClass, jsig, dcsig, fMethodOptions, fOptimizationHints);

        fJavaSignature = jsig.toString();
        fDCSignature = dcsig.toString();
    }
    /*
    void* MethodCallInfo::GetCallback()
    {
        //test();
        if (!fCallback) {
            void* userdata = this;
    #ifdef _WIN64
            static bool allowRawAdapters = true;//!getenv("NO_RAW_FWD");
            if (allowRawAdapters && fIsAdaptableAsRaw) {
                fCallback = (DCCallback*)dcRawCallAdapterSkipTwoArgs((void (*)())fForwardedSymbol);
            }
            else
    #endif
            if (!fCallback)
            {
                fCallback = dcNewCallback(GetDCSignature().c_str(), JavaToNativeCallHandler, userdata);
            }
            */
    boolean getBoolAnnotation(Class<? extends Annotation> ac, AnnotatedElement element, Annotation... directAnnotations) {
        Annotation ann = DynCall.getAnnotation(ac, element, directAnnotations);
        return ann != null;
    }
    private void GetOptions(Options out, Method method, Annotation... directAnnotations) {
        out.bIsWideChar = getBoolAnnotation(Wide.class, method, directAnnotations);
        out.bIsByValue = getBoolAnnotation(ByValue.class, method, directAnnotations);
        out.bIsConst = getBoolAnnotation(Const.class, method, directAnnotations);
        out.bIsSizeT = getBoolAnnotation(NativeSize.class, method, directAnnotations);
        out.bIsCLong = getBoolAnnotation(CLong.class, method, directAnnotations);

        Virtual virtual = DynCall.getAnnotation(Virtual.class, method, directAnnotations);
        out.virtualIndex = virtual == null ? -1 : virtual.value();
    }
}
