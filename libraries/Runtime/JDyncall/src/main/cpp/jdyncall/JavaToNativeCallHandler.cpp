#include "jdyncall.hpp"

using namespace std;

template <typename T>
class scoped_ptr {
	T* ptr;
public:
	scoped_ptr(T* t) : ptr(t) {}
	~scoped_ptr() { delete ptr; }
};

char __cdecl JavaToNativeCallHandler(DCCallback*, DCArgs* args, DCValue* result, void* userdata)
{
	if (!userdata) {
		cerr << "MethodCallHandler was called with a null userdata !!!\n";
		return DC_SIGCHAR_VOID;
	}
	
	MethodCallInfo *info = (MethodCallInfo*)userdata;

	JNIEnv *env = (JNIEnv*)dcArgs_pointer(args);
	jobject objOrClass = (jobject)dcArgs_pointer(args);

	size_t nArgs = info->fArgTypes.size();

	THREAD_STATIC DCCallVM* vm = NULL;
	THREAD_STATIC vector<jobject> *arrays = NULL;
	THREAD_STATIC vector<void*> *arraysTempElements = NULL;
	THREAD_STATIC vector<jint> *arraysTempElementsReleaseModes = NULL;
	typedef void (*ArrayTempElementsReleaseFunc)(JNIEnv *env, jobject array, void* elements, jint mode); 
	THREAD_STATIC vector<ArrayTempElementsReleaseFunc> *arraysReleaseFuncs = NULL;

	if (!vm) {
		vm = dcNewCallVM(1024);
		arrays = new vector<jobject>();
		arraysTempElements = new vector<void*>();
		arraysTempElementsReleaseModes = new vector<jint>();
		arraysReleaseFuncs = new vector<ArrayTempElementsReleaseFunc>();
	} else {
		// reset is done by dcMode anyway ! dcReset(vm);
	}

	dcMode(vm, info->fDCMode);

	if (!info->fIsStatic) {
		jlong address = env->CallLongMethod(objOrClass, Addressable_getAddress);
		dcArgPointer(vm, (void*)address);
	}

	for (size_t iArg = 0; iArg < nArgs; iArg++) {
		ValueType type = info->fArgTypes[iArg];
		Options& options = info->fArgOptions[iArg];

		jobject buffer = NULL;
		jobject array = NULL;
		void* bufferAddress = NULL;
		if (type == eBufferValue)
		{
			buffer = (jobject)dcArgs_pointer(args);
			bufferAddress = env->GetDirectBufferAddress(buffer);
			if (!bufferAddress)
			{
				if (env->IsInstanceOf(buffer, IntBuffer_class)) {
					array = env->CallObjectMethod(buffer, IntBuffer_array);
					type = eIntArrayValue;
				} else if (env->IsInstanceOf(buffer, FloatBuffer_class)) {
					array = env->CallObjectMethod(buffer, FloatBuffer_array);
					type = eFloatArrayValue;
				} else
					cerr << "Implement ME !\n";
			}
		}
					
		switch (type) {
			case eAddressableValue:
			case eCallbackValue:
				{
					jobject obj = (jobject)dcArgs_pointer(args);
					jlong address = obj ? env->CallLongMethod(obj, Addressable_getAddress) : 0;
					if (!address && obj && type == eCallbackValue) {
						address = BindCallback(obj); // synchronizes around obj
					}
					dcArgPointer(vm, (void*)address);
				}
				break;
			case eBufferValue:
				dcArgPointer(vm, bufferAddress);
				break;
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
			case eLongPtrValue:
				dcArgPointer(vm, (void*)(ptrdiff_t)dcArgs_longlong(args));
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
			//ARRAY_VALUE_CASE(char, Char)
			case eCharArrayValue:
			case eWCharValue:
			case eVoidValue:
			case eArrayValue:
			default:
				cerr << "ValueType not supported yet: " << (int)type << " !\n";
		}
	}
	void* cb = info->fForwardedSymbol;
	char returnVal = DC_SIGCHAR_VOID;
	switch (info->fReturnType) {
		case eAddressableValue:
		case eArrayValue:
		case eCallbackValue:
			{
				void* res = dcCallPointer(vm, cb);
				if (!res)
					result->p = NULL;
				else
				{
					//if (!info->fAddressableReturnFactory)
					//	info->CreateAddressableReturnFactory();
				
					//TODO try to reuse instances from arguments
					jobject obj = env->CallObjectMethod(info->fAddressableReturnFactory, AddressableFactory_newInstance, (jlong)res);
					result->p = obj;
				}
				returnVal = DC_SIGCHAR_POINTER;
				break;
			}
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
		case eLongPtrValue:
			result->l = (ptrdiff_t)dcCallPointer(vm, cb);
			returnVal = DC_SIGCHAR_LONGLONG;
			break;
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
		case eCharArrayValue:
		case eBooleanArrayValue:
		case eIntArrayValue:
		case eLongArrayValue:
		case eByteArrayValue:
		case eShortArrayValue:
		case eFloatArrayValue:
		case eDoubleArrayValue:
			cerr << "Array return ValueType not supported: " << (int)info->fReturnType << " !\n";
			break;
		case eBufferValue:
			// TODO
		case eWCharValue:
			// TODO
		default:
			cerr << "Return ValueType not supported yet: " << (int)info->fReturnType << " !\n";
	}
	
#if 0
#ifdef _DEBUG
	size_t dcSigLen = info->fDCSignature.size();
	if (dcSigLen) {
		char theoReturn = info->fDCSignature[dcSigLen - 1];
		if (theoReturn != returnVal) 
			cerr << "Actual return type differs from that declared in signature : '" << returnVal << "' vs. '" << theoReturn << "'\n";
	}
#endif
#endif

	size_t nArrays = arrays->size();
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
	}

	return returnVal;
}