package com.nativelibs4java.opencl.generator;

import com.nativelibs4java.opencl.*;
import com.ochafik.io.IOUtils;
import com.ochafik.lang.jnaerator.ClassOutputter;
import com.ochafik.lang.jnaerator.DeclarationsConverter;
import com.ochafik.lang.jnaerator.GlobalsGenerator;
import com.ochafik.lang.jnaerator.JNAerator;
import com.ochafik.lang.jnaerator.JNAeratorConfig;
import com.ochafik.lang.jnaerator.JNAeratorParser;
import com.ochafik.lang.jnaerator.ObjectiveCGenerator;
import com.ochafik.lang.jnaerator.PreprocessorUtils.MacroUseCallback;
import com.ochafik.lang.jnaerator.Result;
import com.ochafik.lang.jnaerator.Signatures;
import com.ochafik.lang.jnaerator.SourceFiles;
import com.ochafik.lang.jnaerator.TypeConversion;
import com.ochafik.lang.jnaerator.TypeConversion.JavaPrimitive;
import com.ochafik.lang.jnaerator.TypeConversion.TypeConversionMode;
import com.ochafik.lang.jnaerator.UniversalReconciliator;
import com.ochafik.lang.jnaerator.UnsupportedConversionException;
import com.ochafik.lang.jnaerator.parser.Arg;
import com.ochafik.lang.jnaerator.parser.DeclarationsHolder;
import com.ochafik.lang.jnaerator.parser.Declarator;
import com.ochafik.lang.jnaerator.parser.Expression;
import com.ochafik.lang.jnaerator.parser.Function;
import com.ochafik.lang.jnaerator.parser.Identifier;
import com.ochafik.lang.jnaerator.parser.Modifier;
import com.ochafik.lang.jnaerator.parser.SourceFile;
import com.ochafik.lang.jnaerator.parser.Statement;
import com.ochafik.lang.jnaerator.parser.Struct;
import com.ochafik.lang.jnaerator.parser.TypeRef;
import com.ochafik.lang.jnaerator.parser.VariablesDeclaration;
import com.ochafik.lang.jnaerator.runtime.NativeSize;
import com.ochafik.util.listenable.Adapter;
import com.ochafik.util.string.RegexUtils;
import com.ochafik.util.string.StringUtils;
import com.sun.jna.NativeLong;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import static com.ochafik.lang.jnaerator.parser.ElementsHelper.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.util.regex.Pattern;
import org.anarres.cpp.LexerException;

public class JavaCLGenerator extends JNAerator {

    static Pattern nameExtPatt = Pattern.compile("(.*?)\\.(\\w+)");

    public JavaCLGenerator(JNAeratorConfig config) {
		super(config);

        config.noMangling = true;
        config.noCPlusPlus = true;
        config.genCPlusPlus = false;
        config.fileToLibrary = new Adapter<File, String>() {

            @Override
            public String adapt(File value) {
                String[] m = RegexUtils.match(value.getName(), nameExtPatt);
                return m == null ? null : m[1];
            }
        };
        config.functionsAccepter = new Adapter<Function, Boolean>() {

            @Override
            public Boolean adapt(Function value) {
                List<Modifier> mods = value.getModifiers();
                if (Modifier.__kernel.isContainedBy(mods))
                    return true;
                if (value.getValueType() == null)
                    return null;
                mods = value.getValueType().getModifiers();
                return Modifier.__kernel.isContainedBy(mods);
            }
        };
	}

    Map<String, Set<String>> macrosByFile = new HashMap<String, Set<String>>();
    public SourceFiles parseSources(Feedback feedback, TypeConversion typeConverter) throws IOException, LexerException {
		feedback.setStatus("Parsing native headers...");
		return JNAeratorParser.parse(config, typeConverter, new MacroUseCallback() {

            @Override
            public void macroUsed(String path, String macroName) {
                Set<String> macros = macrosByFile.get(path);
                if (macros == null)
                    macrosByFile.put(path, macros = new HashSet<String>());
                macros.add(macroName);
            }
        });
	}

    static Set<String> openclPrimitives = new HashSet<String>();
    static {
        openclPrimitives.add("half");
        openclPrimitives.add("image2d_t");
        openclPrimitives.add("image3d_t");
        openclPrimitives.add("sampler_t");
        openclPrimitives.add("event_t");
    }

    @Override
    public Result createResult(final ClassOutputter outputter, Feedback feedback) {
        return new Result(config, outputter, feedback) {

            @Override
            public void init() {
                typeConverter = new TypeConversion(this) {

                    @Override
                    public void initTypes() {
                        super.initTypes();

                    }

                    
                    @Override
                    public boolean isObjCppPrimitive(String s) {
                        int len;
                        if (s == null || (len = s.length()) == 0)
                            return false;
                        
                        if (super.isObjCppPrimitive(s))
                            return true;
                        if (len > 1 && Character.isDigit(s.charAt(len - 1))) {
                            String ss = s.substring(0, len - 1);
                            if (ss.charAt(0) == 'u')
                                ss = ss.substring(1);
                            
                            if (super.isObjCppPrimitive(ss))
                                return true;
                        }

                        return openclPrimitives.contains(s);
                    }

                };
                declarationsConverter = new DeclarationsConverter(this) {

                    @Override
                    public void convertFunction(Function function, Signatures signatures, boolean isCallback, DeclarationsHolder out, Identifier libraryClassName) {
                        if (isCallback)
                            return;

                        List<Arg> args = function.getArgs();
                        List<Arg> convArgs = new ArrayList<Arg>(args.size());
                        String queueName = "commandQueue";
                        convArgs.add(new Arg(queueName, typeRef(CLQueue.class)));
                        List<Expression> convArgExpr = new ArrayList<Expression>(args.size());
                        int iArg = 1;
                        for (Arg arg : args) {
                            TypeRef tr = arg.createMutatedType();
                            if (tr == null)
                                return;

                            try {
                                TypeRef convTr = convertTypeToJavaCL(result, tr, TypeConversion.TypeConversionMode.PrimitiveOrBufferParameter, null);
                                String convTrStr = convTr.toString();
                                Expression argExpr;
                                String argName = arg.getName() == null ? "arg" + iArg : arg.getName();
                                if (convTrStr.equals(NativeSize.class.getName()) || convTrStr.equals(NativeLong.class.getName()))
                                    argExpr = new Expression.New(tr, varRef(argName));
                                else
                                    argExpr = varRef(ident(argName));

                                convArgs.add(new Arg(argName, tr));

                                convArgExpr.add(varRef(argName));

                            } catch (UnsupportedConversionException ex) {
                                out.addDeclaration(skipDeclaration(function, ex.toString()));
                            }
                            iArg++;
                        }

                        String globalWSName = "globalWorkSizes", localWSName = "localWorkSizes", eventsName = "eventsToWaitFor";
                        convArgs.add(new Arg(globalWSName, typeRef(int[].class)));
                        convArgs.add(new Arg(localWSName, typeRef(int[].class)));
                        convArgs.add(new Arg(eventsName, typeRef(CLEvent.class)));
                        convArgs.add(Arg.createVarArgs());

                        String functionName = function.getName().toString();
                        String kernelVarName = functionName + "_kernel";
                        out.addDeclaration(new VariablesDeclaration(typeRef(CLKernel.class), new Declarator.DirectDeclarator(kernelVarName)));
                        Function method = new Function(Function.Type.JavaMethod, ident(functionName), typeRef(CLEvent.class));
                        method.addModifiers(Modifier.Public, Modifier.Synchronized);
                        method.setArgs(convArgs);
                        method.setBody(block(
                            new Statement.If(
                                expr(varRef(kernelVarName), Expression.BinaryOperator.IsEqual, new Expression.NullExpression()),
                                stat(
                                    expr(
                                        varRef(kernelVarName), Expression.AssignmentOperator.Equal,
                                        methodCall(
                                            "createKernel",
                                            new Expression.Constant(Expression.Constant.Type.String, functionName)
                                        )
                                    )
                                ),
                                null
                            ),
                            stat(methodCall(
                                varRef(kernelVarName),
                                Expression.MemberRefStyle.Dot,
                                "setArgs",
                                convArgExpr.toArray(new Expression[convArgExpr.size()])
                            )),
                            new Statement.Return(methodCall(
                                varRef(kernelVarName),
                                Expression.MemberRefStyle.Dot,
                                "enqueueNDRange",
                                varRef(queueName),
                                varRef(globalWSName),
                                varRef(localWSName),
                                varRef(eventsName)
                            ))
                        ));
                        out.addDeclaration(method);
                    }
                };
                globalsGenerator = new GlobalsGenerator(this);
                objectiveCGenerator = new ObjectiveCGenerator(this);
                universalReconciliator = new UniversalReconciliator();
            }

        };
    }

    static class CLPrim {
        TypeConversion.JavaPrimitive javaPrim;
        int arity;
        boolean isLong, isShort;
        Expression assertExpr;
        Statement checkStatement;
        Expression convertStatement;
        Class<?> argClass;

        public CLPrim(JavaPrimitive javaPrim, int arity) {
            this.javaPrim = javaPrim;
            this.arity = arity;
        }
        
        static Pattern patt = Pattern.compile("(?:(long|short)\\s+)?(float|double|u?(?:char|long|short|int))(\\d)");
        public static CLPrim parse(Result result, TypeRef tr) {
            String s = tr.toString();
            if (s == null || s.length() == 0)
                return null;
            char c = s.charAt(s.length() - 1);
            if (!Character.isDigit(c)) {
                //JavaPrim prim = result.typeConverter.getPrimitive(
                return null;
            }
            String[] m = RegexUtils.match(tr.toString(), patt);
            if (m == null)
                return null;


            //boolean isShort = false,
            //result.typeConverter
            return null;
        }
    }

    private TypeRef convertTypeToJavaCL(Result result, TypeRef valueType, TypeConversionMode typeConversionMode, Identifier libraryClassName) throws UnsupportedConversionException {
        TypeRef original = valueType;
		valueType = result.typeConverter.resolveTypeDef(valueType, libraryClassName, true);

        return valueType;
    }

    @Override
    protected void generateLibraryFiles(SourceFiles sourceFiles, Result result) throws IOException {
        //super.generateLibraryFiles(sourceFiles, result);
        for (SourceFile sourceFile : sourceFiles.getSourceFiles()) {
            String srcFilePath = result.config.relativizeFileForSourceComments(sourceFile.getElementFile());
            File srcFile = new File(srcFilePath);
            String srcParent = srcFile.getParent();
            String srcFileName = srcFile.getName();
            String[] nameExt = RegexUtils.match(srcFileName, nameExtPatt);
            if (nameExt == null)
                continue;
            String name = nameExt[1], ext = nameExt[2];
            if (!ext.equals("c") && !ext.equals("cl"))
                continue;

            String className = (srcParent == null || srcParent.length() == 0 ? "" : srcParent.replace('/', '.').replace('\\', '.') + ".") + name;


            Struct interf = new Struct();
			interf.addToCommentBefore("Wrapper around the OpenCL program " + name);
			interf.addModifiers(Modifier.Public);
			interf.setTag(ident(name));
			Identifier libSuperInter = ident(CLAbstractUserProgram.class);
			interf.setType(Struct.Type.JavaClass);

            //result.declarationsConverter.convertStructs(null, null, interf, null)
            Signatures signatures = new Signatures();//result.getSignaturesForOutputClass(fullLibraryClassName);
			result.typeConverter.allowFakePointers = true;
            String library = name;
            Identifier fullLibraryClassName = ident(className);
			result.declarationsConverter.convertEnums(result.enumsByLibrary.get(library), signatures, interf, fullLibraryClassName);
			result.declarationsConverter.convertConstants(library, result.definesByLibrary.get(library), sourceFiles, signatures, interf, fullLibraryClassName);
			result.declarationsConverter.convertStructs(result.structsByLibrary.get(library), signatures, interf, fullLibraryClassName);
			//result.declarationsConverter.convertCallbacks(result.callbacksByLibrary.get(library), signatures, interf, fullLibraryClassName);
			result.declarationsConverter.convertFunctions(result.functionsByLibrary.get(library), signatures, interf, fullLibraryClassName);

            //for ()

    /*
            public SampleUserProgram(CLContext context) throws IOException {
        super(context, readRawSourceForClass(SampleUserProgram.class));
    }*/


            for (Set<String> set : macrosByFile.values()) {
                for (String macroName : set) {
                    String[] parts = macroName.split("_+");
                    List<String> newParts = new ArrayList<String>(parts.length);
                    for (String part : parts) {
                        if (part == null || (part = part.trim()).length() == 0)
                            continue;
                        newParts.add(StringUtils.capitalize(part));
                    }
                    String functionName = "define" + StringUtils.implode(newParts, "");
                    Function macroDef = new Function(Function.Type.JavaMethod, ident(functionName), typeRef("void"));
                    String valueName = "value";
                    macroDef.addArg(new Arg(valueName, typeRef(String.class)));
                    macroDef.setBody(block(stat(methodCall("defineMacro", expr(Expression.Constant.Type.String, macroName), varRef(valueName)))));
                    interf.addDeclaration(macroDef);
                }
            }

            PrintWriter out = result.classOutputter.getClassSourceWriter(className);
            out.println(interf);
            out.close();
        }
    }

    
    @Override
    protected void autoConfigure() {
        super.autoConfigure();

            /*
        __OPENCL_VERSION__
        __ENDIAN_LITTLE__

        __IMAGE_SUPPORT__
        __FAST_RELAXED_MATH__
        */

    }

    public static void main(String[] args) {
        JNAerator.main(new JavaCLGenerator(new JNAeratorConfig()),
            new String[] {
                "-o", "/Users/ochafik/Prog/Java/versionedSources/nativelibs4java/trunk/libraries/OpenCL/Demos/target/generated-sources/main/java",
                "-noJar",
                "-noComp",
                "-v",
                "-addRootDir", "/Users/ochafik/Prog/Java/versionedSources/nativelibs4java/trunk/libraries/OpenCL/Demos/target/../src/main/opencl",
                "/Users/ochafik/Prog/Java/versionedSources/nativelibs4java/trunk/libraries/OpenCL/Demos/target/../src/main/opencl/com/nativelibs4java/opencl/demos/ParticlesDemo.c"
            }
        );
	}
}

