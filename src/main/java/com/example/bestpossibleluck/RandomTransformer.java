package com.example.bestpossibleluck;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class RandomTransformer implements IClassTransformer {
    private static final String RANDOM_CLASS = "java/util/Random";
    private static final String THREAD_LOCAL_RANDOM_CLASS = "java/util/concurrent/ThreadLocalRandom";
    private static final String MATH_CLASS = "java/lang/Math";
    private static final String STRICT_MATH_CLASS = "java/lang/StrictMath";
    private static final String HOOKS = "com/example/bestpossibleluck/LuckHooks";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) {
            return null;
        }

        if (RANDOM_CLASS.equals(name)) {
            return transformRandom(name, basicClass);
        }
        if (THREAD_LOCAL_RANDOM_CLASS.equals(name)) {
            return transformThreadLocalRandom(name, basicClass);
        }
        if (MATH_CLASS.equals(name) || STRICT_MATH_CLASS.equals(name)) {
            return transformMath(name, basicClass);
        }
        return basicClass;
    }

    private byte[] transformRandom(String name, byte[] basicClass) {
        BestPossibleLuckMod.LOGGER.info("Transforming {} to force high-roll RNG outcomes.", name);
        ClassReader reader = new ClassReader(basicClass);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        reader.accept(new RedirectingClassVisitor(writer, name), 0);
        return writer.toByteArray();
    }

    private byte[] transformThreadLocalRandom(String name, byte[] basicClass) {
        BestPossibleLuckMod.LOGGER.info("Transforming {} to force high-roll RNG outcomes.", name);
        ClassReader reader = new ClassReader(basicClass);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        reader.accept(new RedirectingClassVisitor(writer, name), 0);
        return writer.toByteArray();
    }

    private byte[] transformMath(String name, byte[] basicClass) {
        BestPossibleLuckMod.LOGGER.info("Transforming {} to force Math.random() high-roll outcomes.", name);
        ClassReader reader = new ClassReader(basicClass);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        reader.accept(new RedirectingClassVisitor(writer, name), 0);
        return writer.toByteArray();
    }

    private static class RedirectingClassVisitor extends ClassVisitor {
        private final String ownerName;

        RedirectingClassVisitor(ClassVisitor cv, String ownerName) {
            super(Opcodes.ASM5, cv);
            this.ownerName = ownerName;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            RedirectTarget target = RedirectTarget.find(ownerName, name, desc);
            if (target != null) {
                return replaceWithStatic(access, name, desc, signature, exceptions, target.hookName, target.hookDesc);
            }
            return super.visitMethod(access, name, desc, signature, exceptions);
        }

        private MethodVisitor replaceWithStatic(int access, String name, String desc, String signature, String[] exceptions, String hookName, String hookDesc) {
            MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
            mv.visitCode();

            Type[] argumentTypes = Type.getArgumentTypes(desc);
            int localIndex = (access & Opcodes.ACC_STATIC) != 0 ? 0 : 1;
            for (Type argumentType : argumentTypes) {
                mv.visitVarInsn(argumentType.getOpcode(Opcodes.ILOAD), localIndex);
                localIndex += argumentType.getSize();
            }

            mv.visitMethodInsn(Opcodes.INVOKESTATIC, HOOKS, hookName, hookDesc, false);
            mv.visitInsn(Type.getReturnType(desc).getOpcode(Opcodes.IRETURN));
            mv.visitMaxs(0, 0);
            mv.visitEnd();
            return null;
        }
    }

    private static final class RedirectTarget {
        private final String owner;
        private final String name;
        private final String desc;
        private final String hookName;
        private final String hookDesc;

        private static final RedirectTarget[] TARGETS = new RedirectTarget[] {
            new RedirectTarget(RANDOM_CLASS, "next", "(I)I", "bestNext", "(I)I"),
            new RedirectTarget(RANDOM_CLASS, "nextInt", "(I)I", "bestNextInt", "(I)I"),
            new RedirectTarget(RANDOM_CLASS, "nextInt", "()I", "bestNextIntUnbounded", "()I"),
            new RedirectTarget(RANDOM_CLASS, "nextLong", "()J", "bestNextLong", "()J"),
            new RedirectTarget(RANDOM_CLASS, "nextBoolean", "()Z", "bestNextBoolean", "()Z"),
            new RedirectTarget(RANDOM_CLASS, "nextFloat", "()F", "bestNextFloat", "()F"),
            new RedirectTarget(RANDOM_CLASS, "nextDouble", "()D", "bestNextDouble", "()D"),
            new RedirectTarget(RANDOM_CLASS, "nextGaussian", "()D", "bestNextGaussian", "()D"),
            new RedirectTarget(RANDOM_CLASS, "nextBytes", "([B)V", "bestNextBytes", "([B)V"),
            new RedirectTarget(RANDOM_CLASS, "setSeed", "(J)V", "touchSeed", "(J)V"),
            new RedirectTarget(THREAD_LOCAL_RANDOM_CLASS, "nextInt", "(I)I", "bestNextInt", "(I)I"),
            new RedirectTarget(THREAD_LOCAL_RANDOM_CLASS, "nextInt", "(II)I", "bestNextIntRange", "(II)I"),
            new RedirectTarget(THREAD_LOCAL_RANDOM_CLASS, "nextLong", "()J", "bestNextLong", "()J"),
            new RedirectTarget(THREAD_LOCAL_RANDOM_CLASS, "nextLong", "(J)J", "bestNextLongBounded", "(J)J"),
            new RedirectTarget(THREAD_LOCAL_RANDOM_CLASS, "nextLong", "(JJ)J", "bestNextLongRange", "(JJ)J"),
            new RedirectTarget(THREAD_LOCAL_RANDOM_CLASS, "nextBoolean", "()Z", "bestNextBoolean", "()Z"),
            new RedirectTarget(THREAD_LOCAL_RANDOM_CLASS, "nextFloat", "()F", "bestNextFloat", "()F"),
            new RedirectTarget(THREAD_LOCAL_RANDOM_CLASS, "nextDouble", "()D", "bestNextDouble", "()D"),
            new RedirectTarget(THREAD_LOCAL_RANDOM_CLASS, "nextDouble", "(D)D", "bestNextDoubleBounded", "(D)D"),
            new RedirectTarget(THREAD_LOCAL_RANDOM_CLASS, "nextDouble", "(DD)D", "bestNextDoubleRange", "(DD)D"),
            new RedirectTarget(MATH_CLASS, "random", "()D", "bestMathRandom", "()D"),
            new RedirectTarget(STRICT_MATH_CLASS, "random", "()D", "bestMathRandom", "()D")
        };

        private RedirectTarget(String owner, String name, String desc, String hookName, String hookDesc) {
            this.owner = owner;
            this.name = name;
            this.desc = desc;
            this.hookName = hookName;
            this.hookDesc = hookDesc;
        }

        private static RedirectTarget find(String owner, String name, String desc) {
            for (RedirectTarget target : TARGETS) {
                if (target.owner.equals(owner) && target.name.equals(name) && target.desc.equals(desc)) {
                    return target;
                }
            }
            return null;
        }
    }
}
