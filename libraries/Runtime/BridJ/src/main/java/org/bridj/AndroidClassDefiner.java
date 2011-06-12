/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bridj;

import com.android.dx.dex.cf.CfOptions;
import com.android.dx.dex.cf.CfTranslator;
import com.android.dx.dex.code.PositionList;
import com.android.dx.dex.file.ClassDefItem;
import com.android.dx.dex.file.DexFile;
import com.android.dx.rop.cst.CstType;
import com.android.dx.rop.cst.CstUtf8;
import dalvik.system.DexClassLoader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ochafik
 */
class AndroidClassDefiner extends DexClassLoader implements ClassDefiner {

    public AndroidClassDefiner() {
        super(null, null, null, AndroidClassDefiner.class.getClassLoader());
    }
    
    public Class<?> defineClass(String className, byte[] data) throws ClassFormatError {
        DexFile dexFile = new DexFile();
        dexFile.add(CfTranslator.translate(className.replace('.', '/') + ".class", data, new CfOptions()));
        try {
            StringWriter out = new StringWriter();
            byte[] dexData = dexFile.toDex(out, false);
            if (BridJ.debug)
                BridJ.log(Level.INFO, "Dex output for class " + className + " : " + out.toString());
            //System.out.println("#\n# Converted class data to " + dexData.length + " bytes of dex data\n#");
            return defineClass(className, dexData, 0, dexData.length);
        } catch (IOException ex) {
            throw new ClassFormatError("Unable to convert class data to Dalvik code using Dex : " + ex);
        }
    }
}
