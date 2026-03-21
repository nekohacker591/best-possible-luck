package com.example.bestpossibleluck;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class RandomTransformer implements IClassTransformer {
    private static final String RANDOM_CLASS = "java/util/Random";
    private static final String HOOKS = "com/example/bestpossibleluck/LuckHooks";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (!RANDOM_CLASS.equals(name) || basicClass == null) {
            return basicClass;
        }

        BestPossibleLuckMod.LOGGER.info("Transforming {} to force high-roll RNG outcomes.", name);
        ClassReader reader = new ClassReader(basicClass);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        ClassVisitor visitor = new RandomClassVisitor(writer);
        reader.accept(visitor, 0);
        return writer.toByteArray();
    }

    private static class RandomClassVisitor extends ClassVisitor {
        RandomClassVisitor(ClassVisitor cv) {
            super(Opcodes.ASM5, cv);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            if ("nextInt".equals(name) && "(I)I".equals(desc)) {
                return replaceWithStatic(access, name, desc, signature, exceptions, HOOKS, "bestNextInt", "(I)I");
            }
            if ("nextInt".equals(name) && "()I".equals(desc)) {
                return replaceWithStatic(access, name, desc, signature, exceptions, HOOKS, "bestNextIntUnbounded", "()I");
            }
            if ("nextLong".equals(name) && "()J".equals(desc)) {
                return replaceWithStatic(access, name, desc, signature, exceptions, HOOKS, "bestNextLong", "()J");
            }
            if ("nextBoolean".equals(name) && "()Z".equals(desc)) {
                return replaceWithStatic(access, name, desc, signature, exceptions, HOOKS, "bestNextBoolean", "()Z");
            }
            if ("nextFloat".equals(name) && "()F".equals(desc)) {
                return replaceWithStatic(access, name, desc, signature, exceptions, HOOKS, "bestNextFloat", "()F");
            }
            if ("nextDouble".equals(name) && "()D".equals(desc)) {
                return replaceWithStatic(access, name, desc, signature, exceptions, HOOKS, "bestNextDouble", "()D");
            }
            if ("nextGaussian".equals(name) && "()D".equals(desc)) {
                return replaceWithStatic(access, name, desc, signature, exceptions, HOOKS, "bestNextGaussian", "()D");
            }
            if ("setSeed".equals(name) && "(J)V".equals(desc)) {
                return replaceWithStatic(access, name, desc, signature, exceptions, HOOKS, "touchSeed", "(J)V");
            }

            return super.visitMethod(access, name, desc, signature, exceptions);
        }

        private MethodVisitor replaceWithStatic(int access, String name, String desc, String signature, String[] exceptions, String owner, String hookName, String hookDesc) {
            MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
            mv.visitCode();

            int slot = ((access & Opcodes.ACC_STATIC) != 0) ? 0 : 1;
            for (int i = 0; i < hookDesc.indexOf(')'); i++) {
                char ch = hookDesc.charAt(i);
                if (ch == '(' || ch == ')') {
                    continue;
                }
                if (ch == 'J' || ch == 'D') {
                    mv.visitVarInsn(ch == 'J' ? Opcodes.LLOAD : Opcodes.DLOAD, slot);
                    slot += 2;
                    break;
                }
                if (ch == 'I' || ch == 'Z' || ch == 'F') {
                    mv.visitVarInsn(ch == 'F' ? Opcodes.FLOAD : Opcodes.ILOAD, slot);
                    slot += 1;
                    break;
                }
            }

            mv.visitMethodInsn(Opcodes.INVOKESTATIC, owner, hookName, hookDesc, false);
            switch (desc.charAt(desc.length() - 1)) {
                case 'V':
                    mv.visitInsn(Opcodes.RETURN);
                    break;
                case 'J':
                    mv.visitInsn(Opcodes.LRETURN);
                    break;
                case 'D':
                    mv.visitInsn(Opcodes.DRETURN);
                    break;
                case 'F':
                    mv.visitInsn(Opcodes.FRETURN);
                    break;
                case 'Z':
                case 'I':
                    mv.visitInsn(Opcodes.IRETURN);
                    break;
                default:
                    throw new IllegalStateException("Unsupported descriptor: " + desc);
            }
            mv.visitMaxs(2, 3);
            mv.visitEnd();
            return null;
        }
    }
}
