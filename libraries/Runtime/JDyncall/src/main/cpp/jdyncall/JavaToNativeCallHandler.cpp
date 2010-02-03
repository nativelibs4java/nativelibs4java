#include "jdyncall.hpp"
//#include <iostream>

using namespace std;

template <typename T>
class scoped_ptr {
	T* ptr;
public:
	scoped_ptr(T* t) : ptr(t) {}
	~scoped_ptr() { delete ptr; }
};

char __cdecl doJavaToNativeCallHandler(DCArgs* args, DCValue* result, MethodCallInfo *info)
{
	JNIEnv *env = (JNIEnv*)dcArgs_pointer(args);
	jobject objOrClass = (jobject)dcArgs_pointer(args);

	size_t nArgs = info->nParams;

	THREAD_STATIC DCCallVM* vm = NULL;
	//THREAD_STATIC vector<jobject> *arrays = NULL;
	//THREAD_STATIC vector<void*> *arraysTempElements = NULL;
	//THREAD_STATIC vector<jint> *arraysTempElementsReleaseModes = NULL;
	//typedef void (*ArrayTempElementsReleaseFunc)(JNIEnv *env, jobject array, void* elements, jint mode); 
	//THREAD_STATIC vector<ArrayTempElementsReleaseFunc> *arraysReleaseFuncs = NULL;

	if (!vm) {
		vm = dcNewCallVM(1024);
		//arrays = new vector<jobject>();
		//arraysTempElements = new vector<void*>();
		//arraysTempElementsReleaseModes = new vector<jint>();
		//arraysReleaseFuncs = new vector<ArrayTempElementsReleaseFunc>();
	} else {
		// reset is done by dcMode anyway ! dcReset(vm);
	}

	dcMode(vm, info->fDCMode);

	/*if (!info->fIsStatic) {
		jlong address = env->CallLongMethod(objOrClass, Pointable_getPointer);
		dcArgPointer(vm, (void*)address);
	}*/

	for (size_t iArg = 0; iArg < nArgs; iArg++) {
		ValueType type = info->fParamTypes[iArg];
		switch (type) {
			case eIntValue:
				dcArgInt(vm, dcArgs_int(args));
				break;
			case eCLongValue:
				dcArgLong(vm, (long)dcArgs_longlong(args));
				break;
			case eSizeTValue:
				if (sizeof(size_t) == 4)
					dcArgInt(vm, (int)dcArgs_longlong(args));
				else
					dcArgLongLong(vm, dcArgs_longlong(args));
				break;
			case eLongValue:
				dcArgLongLong(vm, dcArgs_longlong(args));
				break;
			case eShortValue:
				dcArgShort(vm, dcArgs_short(args));
				break;
			case eByteValue:
				dcArgChar(vm, dcArgs_char(args));
				break;
			case eFloatValue:
				dcArgFloat(vm, dcArgs_float(args));
				break;
			case eDoubleValue:
				dcArgDouble(vm, dcArgs_double(args));
				break;
				/*
#define ARRAY_VALUE_CASE(lowCase, capCase) \
			case e ## capCase ## ArrayValue: \
			{ \
				if (!array) \
					array = (jobject)dcArgs_pointer(args); \
				void* elements = env->Get ## capCase ## ArrayElements((j ## lowCase ## Array)array, NULL); \
				arrays->push_back(array); \
				arraysReleaseFuncs->push_back((ArrayTempElementsReleaseFunc)env->functions->Release ## capCase ## ArrayElements); \
				arraysTempElements->push_back(elements); \
				arraysTempElementsReleaseModes->push_back(options.bIsConst ? JNI_ABORT : 0); \
				dcArgPointer(vm, elements); \
				break; \
			}
			ARRAY_VALUE_CASE(int, Int)
			ARRAY_VALUE_CASE(long, Long)
			ARRAY_VALUE_CASE(short, Short)
			ARRAY_VALUE_CASE(byte, Byte)
			ARRAY_VALUE_CASE(float, Float)
			ARRAY_VALUE_CASE(double, Double)
			ARRAY_VALUE_CASE(boolean, Boolean)
			//ARRAY_VALUE_CASE(char, Char)*/
			//case eVoidValue:
			//default:
				//ASSERT(0);
				//cerr << "ValueType not supported yet: " << (int)type << " !\n";
		}
	}
	void* cb = info->fForwardedSymbol;
	char returnVal = DC_SIGCHAR_VOID;
	switch (info->fReturnType) {
#define CALL_CASE(valueType, capCase, hiCase, uni) \
		case valueType: \
			result->uni = dcCall ## capCase(vm, cb); \
			returnVal = DC_SIGCHAR_ ## hiCase; \
			break;
		CALL_CASE(eIntValue, Int, INT, i)
		CALL_CASE(eLongValue, Long, LONGLONG, l)
		CALL_CASE(eShortValue, Short, SHORT, s)
		CALL_CASE(eFloatValue, Float, FLOAT, f)
		CALL_CASE(eDoubleValue, Double, DOUBLE, d)
		CALL_CASE(eByteValue, Char, CHAR, c)
		case eCLongValue:
			result->l = dcCallLong(vm, cb);
			returnVal = DC_SIGCHAR_LONG;
			break;
		case eSizeTValue:
			result->l = (sizeof(size_t) == 4) ? dcCallInt(vm, cb) : dcCallLongLong(vm, cb);
			returnVal = DC_SIGCHAR_LONG;
			break;
		case eVoidValue:
			dcCallVoid(vm, cb);
			returnVal = DC_SIGCHAR_VOID;
			break;
		case eWCharValue:
			// TODO
		default:
			//cerr << "Return ValueType not supported yet: " << (int)info->fReturnType << " !\n";
			returnVal = DC_SIGCHAR_VOID;
	}

	/*size_t nArrays = arrays->size();
	if (nArrays)
	{
		for (size_t iArray = 0; iArray < nArrays; iArray++) {
			ArrayTempElementsReleaseFunc func = (*arraysReleaseFuncs)[iArray];
			func(env, (*arrays)[iArray], (*arraysTempElements)[iArray], (*arraysTempElementsReleaseModes)[iArray]);
		}
		arrays->clear();
		arraysTempElements->clear();
		arraysTempElementsReleaseModes->clear();
		arraysReleaseFuncs->clear();
	}*/

	return returnVal;
}

char __cdecl JavaToNativeCallHandler(DCCallback*, DCArgs* args, DCValue* result, void* userdata)
{
	if (!userdata) {
		//cerr << "MethodCallHandler was called with a null userdata !!!\n";
		return DC_SIGCHAR_VOID;
	}
	
	MethodCallInfo *info = (MethodCallInfo*)userdata;

	BEGIN_TRY();
	return doJavaToNativeCallHandler(args, result, info);	
	END_TRY(info->fEnv);
}
