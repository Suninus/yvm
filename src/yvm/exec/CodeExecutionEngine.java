package yvm.exec;

import rtstruct.YThread;
import rtstruct.meta.MetaClass;
import rtstruct.meta.MetaClassMethod;
import rtstruct.ystack.YStack;
import rtstruct.ystack.YStackFrame;
import ycloader.adt.attribute.Attribute;
import ycloader.adt.u1;
import ycloader.exception.ClassInitializingException;
import yvm.adt.*;

import java.util.ArrayList;

import static java.util.Objects.isNull;
import static yvm.auxil.Common.*;

public final class CodeExecutionEngine {
    private YThread thread;
    private boolean ignited;
    private MetaClass metaClass;

    public CodeExecutionEngine(YThread thread) {
        this.thread = thread;
        ignited = false;
    }

    public void ignite(MetaClass metaClassInfo) {
        this.metaClass = metaClassInfo;
        ignited = true;
    }

    public void executeCLinit() throws ClassInitializingException {
        if (!ignited) {
            throw new ClassInitializingException("code execution engine is not ready");
        }

        Tuple6<String, String, u1[], MetaClassMethod.StackRequirement,
                MetaClassMethod.ExceptionTable[], ArrayList<Attribute>>
                clinit = metaClass.getMethods().findMethod("<clinit>");

        if (isNull(clinit) || strNotEqual(clinit.get1Placeholder(), "<clinit>")) {
            throw new ClassInitializingException("can not find synthetic <clinit> method");
        }

        int maxLocals = clinit.get4Placeholder().maxLocals;
        int maxStack = clinit.get4Placeholder().maxStack;

        YStackFrame frame = new YStackFrame();
        frame.allocateSize(maxStack, maxLocals);
        thread.stack().pushFrame(frame);

        Opcode op = new Opcode(clinit.get3Placeholder());
        op.codes2Opcodes();
        op.debug();
        //codeExecution(op);
    }

    private void codeExecution(Opcode op) throws ClassInitializingException {
        ArrayList<Tuple3<Integer, Integer, Operand>>
                opcodes = op.getOpcodes();
        YStack stack = thread.stack();

        for (int i = 0; i < opcodes.size(); i++) {
            Tuple3 cd = opcodes.get(i);
            int programCount = (Integer) cd.get1Placeholder();
            thread.pc(programCount);
            switch ((Integer) cd.get2Placeholder()) {

                //Load reference from array
                case Mnemonic.aaload: {
                    int index = (int) stack.currentFrame().pop$operand();
                    Object[] arrayRef = (Object[]) stack.currentFrame().pop$operand();

                    if (isNull(arrayRef)) {
                        throw new NullPointerException("reference of an array is null");
                    }
                    if (!inRange(arrayRef, index)) {
                        throw new ArrayIndexOutOfBoundsException("array index " + index + " out of bounds");
                    }
                    stack.currentFrame().push$operand(arrayRef[index]);
                }
                break;

                //Store into reference array
                case Mnemonic.aastore: {
                    Object value = stack.currentFrame().pop$operand();
                    int index = (int) stack.currentFrame().pop$operand();
                    Object[] arrayRef = (Object[]) stack.currentFrame().pop$operand();

                    if (isClass(value.getClass())) {
                        if (!isSameClass(arrayRef.getClass().getComponentType(), value.getClass())
                                && value.getClass().isInstance(arrayRef.getClass().getComponentType())) {
                            throw new ArrayStoreException("incorrect value type to be stored into an array");
                        }
                    }
                    if (isInterface(value.getClass())) {
                        if (isClass(arrayRef.getClass().getComponentType())) {
                            if (!isSameClass(arrayRef.getClass().getComponentType(), Object.class)) {
                                throw new ArrayStoreException("incorrect value type to be stored into an array");
                            }
                        } else if (isInterface(arrayRef.getClass().getComponentType())) {
                            if (!value.getClass().isInstance(arrayRef.getClass().getComponentType())) {
                                throw new ArrayStoreException("incorrect value type to be stored into an array");
                            }
                        }
                    } else if (isArray(value.getClass())) {
                        if (isClass(arrayRef.getClass().getComponentType())) {
                            if (!isSameClass(arrayRef.getClass().getComponentType(), Object.class)) {
                                throw new ArrayStoreException("incorrect value type to be stored into an array");
                            }
                        } else if (isArray(arrayRef.getClass().getComponentType())) {
                            if (!isSameClass(
                                    arrayRef.getClass().getComponentType().getComponentType(),
                                    value.getClass().getComponentType()
                            )) {
                                throw new ArrayStoreException("incorrect value type to be stored into an array");
                            }
                        } else if (isInterface(arrayRef.getClass().getComponentType())) {
                            if (!value.getClass().isInstance(arrayRef.getClass().getComponentType())) {
                                throw new ArrayStoreException("incorrect value type to be stored into an array");
                            }
                        }
                    }

                    arrayRef[index] = value;
                }
                break;

                //Push null
                case Mnemonic.aconst_null: {
                    stack.currentFrame().push$operand(null);
                }
                break;

                //Load reference from local variable
                case Mnemonic.aload: {
                    int index = (int) ((Operand) cd.get3Placeholder()).get0();
                    Object objectRef = stack.currentFrame().getLocalVariable(index);
                    stack.currentFrame().push$operand(objectRef);
                }
                break;

                //Load reference from local variable with index 0
                case Mnemonic.aload_0: {
                    Object objectRef = stack.currentFrame().getLocalVariable(0);
                    stack.currentFrame().push$operand(objectRef);
                }
                break;

                //Load reference from local variable with index 1
                case Mnemonic.aload_1: {
                    Object objectRef = stack.currentFrame().getLocalVariable(1);
                    stack.currentFrame().push$operand(objectRef);
                }
                break;

                //Load reference from local variable with index 2
                case Mnemonic.aload_2: {
                    Object objectRef = stack.currentFrame().getLocalVariable(2);
                    stack.currentFrame().push$operand(objectRef);
                }
                break;

                //Load reference from local variable with index 3
                case Mnemonic.aload_3: {
                    Object objectRef = stack.currentFrame().getLocalVariable(3);
                    stack.currentFrame().push$operand(objectRef);
                }
                break;

                //Create new array of reference
                case Mnemonic.anewarray: {
                    //The count represents the number of components of the array to
                    //be created.
                    int count = (int) stack.currentFrame().pop$operand();
                    byte indexByte1 = (byte) ((Operand) cd.get3Placeholder()).get0();
                    byte indexByte2 = (byte) ((Operand) cd.get3Placeholder()).get1();
                    int index = (indexByte1 << 8) | indexByte2;
                    //todo:create an array of component type denoted by index of constant pool
                    System.out.println(metaClass.getConstantPool().at(index));
                }
                break;
                default:
                    //throw new ClassInitializingException("unknown opcode in execution sequence");
            }
        }
    }
}
