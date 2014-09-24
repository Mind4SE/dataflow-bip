package org.ow2.mind.beam.adl;

import java.util.Map;

import org.ow2.mind.compilation.CompilerContextHelper;

public final class BeamHelper {
    public static final String BIP_FILE_EXT   = ".bip";
    
    public static String getBipFileName(final Map<Object, Object> context) {
        return (String) context.get(CompilerContextHelper.EXECUTABLE_NAME_CONTEXT_KEY) + 
        BIP_FILE_EXT;
      }
}
