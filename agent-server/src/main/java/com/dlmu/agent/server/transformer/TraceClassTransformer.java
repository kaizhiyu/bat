package com.dlmu.agent.server.transformer;

import com.dlmu.agent.server.tclass.Configuration;
import com.dlmu.agent.server.tclass.TraceClass;
import org.objectweb.asm.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

/**
 * @author heipacker on 16-5-15.
 */
public class TraceClassTransformer implements ClassFileTransformer {

    private static final Logger logger = LoggerFactory.getLogger(TraceClassTransformer.class);

    private final Configuration configuration;

    public TraceClassTransformer(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        TraceClass traceClass = configuration.match(className);
        if (traceClass != null) {
            logger.info("instrument class: {}", className);
            ClassReader traceClassReader = new ClassReader(classfileBuffer);

            int flag = ClassWriter.COMPUTE_MAXS;
            //如果低于1.7版本，还是用compute maxs吧
            short version = traceClassReader.readShort(6);
            if (version >= Opcodes.V1_7) {
                flag = ClassWriter.COMPUTE_FRAMES;
            }

            //自动计算stack frame，如果没有开启，如果class是1.7版本的则会抛出java.lang.VerifyError: Expecting a stackmap frame at branch target 这样的异常
            ClassWriter traceClassWriter = new ClassWriter(flag);
            TraceClassVisitor traceClassVisitor = new TraceClassVisitor(/*new CheckClassAdapter*/(traceClassWriter), traceClass, protectionDomain);
            traceClassReader.accept(traceClassVisitor, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

            return traceClassWriter.toByteArray();
        }
        return classfileBuffer;
    }
}
