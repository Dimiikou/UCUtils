package de.fuzzlemann.ucutils.base.command.execution;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import de.fuzzlemann.ucutils.base.abstraction.AbstractionLayer;
import de.fuzzlemann.ucutils.base.abstraction.UPlayer;
import de.fuzzlemann.ucutils.base.command.Command;
import de.fuzzlemann.ucutils.base.command.CommandParam;
import de.fuzzlemann.ucutils.base.command.exceptions.ArgumentException;
import de.fuzzlemann.ucutils.base.text.TextUtils;
import de.fuzzlemann.ucutils.utils.Logger;
import de.fuzzlemann.ucutils.utils.ReflectionUtil;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.ClassUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;

/**
 * @author Fuzzlemann
 */
@SideOnly(Side.CLIENT)
class CommandIssuer {

    private final String label;
    private final String[] args;
    private final boolean throwException;

    private final Object commandExecutor;
    private final Method onCommand;
    private final Command commandAnnotation;

    /**
     * @param label          the label of the command
     * @param args           the given arguments
     * @param throwException {@code true}, if an exception on error should be thrown, {@code false}, when the error-handling should be done by the method itself.
     *                       When this is enabled, asynchronous execution is disabled by default. This should mostly be used for testing.
     */
    public CommandIssuer(String label, String[] args, boolean throwException) {
        this.label = label;
        this.args = args;
        this.throwException = throwException;

        this.commandExecutor = CommandRegistry.COMMAND_REGISTRY.get(label);
        this.onCommand = CommandReflection.getOnCommand(commandExecutor);
        this.commandAnnotation = CommandReflection.getCommand(onCommand);
    }

    /**
     * Issues the command associated with the {@code label}.
     */
    void issue() {
        // Runnable containing the surrounding logic of the command execution
        Runnable commandRunnable = () -> {
            String usage = commandAnnotation.usage();

            try {
                // Sends the usage if the arguments given were incorrect (false is returned)
                if (!executeCommand())
                    sendUsage(usage, label);
            } catch (Throwable e) { // check if the throwable is expected; if so, send the usage
                if (throwException) throw new RuntimeException(e);

                Class<? extends Throwable>[] sendUsageOn = commandAnnotation.sendUsageOn();

                for (Class<? extends Throwable> clazz : sendUsageOn) {
                    if (e.getClass().isAssignableFrom(clazz)) {
                        sendUsage(usage, label); // throwable is expected; send usage
                        return;
                    }
                }

                TextUtils.error("Ein Fehler ist während der Ausführung des Commands aufgetreten.");
                Logger.LOGGER.catching(e);
            }
        };

        // Executes the command in an separate thread when stated
        if (commandAnnotation.async() && !throwException) {
            ForkJoinPool.commonPool().submit(commandRunnable); // asynchronous
        } else {
            commandRunnable.run(); // synchronous
        }
    }

    /**
     * Parses and sends the {@code usage}.
     *
     * @param usage the usage of the command
     * @param label the label the player used for accessing the command
     * @implNote The substring {@code %label%} is replaced with {@code label}
     */
    private void sendUsage(String usage, String label) {
        usage = usage.replace("%label%", label);
        TextUtils.error(usage);
    }

    /**
     * Parses the arguments and passes them on to the command.
     *
     * @return {@code true}, if the execution of the command was successful; {@code false}, if the arguments ({@code args}) were not given correctly
     */
    private boolean executeCommand() throws Throwable {
        Object[] parameters;
        if (CommandReflection.checkDefaultUsage(onCommand)) { // checks if the raw unparsed arguments should be passed on to onCommand
            parameters = new Object[]{args};
        } else {
            try {
                parameters = parseArguments();
            } catch (ArgumentException e) {
                return !e.showUsage(); // arguments failed to parse properly as those were not given correctly
            }
        }

        Object[] checkedParameters;
        if (CommandReflection.hasPlayerParam(onCommand)) { //checks if UPlayer should be passed on to the command
            // the UPlayer parameter *must* always be the first one, so when it is present, the other parameters
            // are getting moved one to the right and the UPlayer is inserted at the start of the (new) array
            checkedParameters = new Object[parameters.length + 1];
            checkedParameters[0] = AbstractionLayer.getPlayer();
            System.arraycopy(parameters, 0, checkedParameters, 1, parameters.length); //Content of the previous array is inserted in the new array at index 1
        } else {
            checkedParameters = parameters; // no player param present -> use previous array
        }

        try {
            return (boolean) onCommand.invoke(commandExecutor, checkedParameters); //executes the command itself
        } catch (IllegalAccessException | IllegalArgumentException e) {
            return false; // the resolved arguments were wrong; the cause is the wrong usage of the command
        } catch (InvocationTargetException e) {
            throw e.getCause(); // the underlying method throws an exception
        }
    }

    /**
     * Parses the arguments to the fitting {@link Object}s.
     *
     * @return the parsed arguments
     * @throws ArgumentException when invalid arguments were given
     */
    private Object[] parseArguments() {
        ListMultimap<Class<?>, CommandParam> commandParamMap = parseParameters(); // the parameter types associated with the CommandParam
        List<Object> arguments = new ArrayList<>(); // the list where all parsed objects land in

        int index = 0;
        for (Map.Entry<Class<?>, CommandParam> entry : commandParamMap.entries()) {
            Class<?> parameterType = entry.getKey();
            CommandParam commandParam = entry.getValue();

            if (commandParam.joinStart() || commandParam.arrayStart()) {
                String[] joiningArray;
                int endIndex = getEndIndexOfArgumentArray(index, commandParamMap);
                if (args.length <= endIndex) { // checks if the end index is outside the argument array
                    if (commandParam.required()) // as an own argument is required, the execution is ended here
                        throw new ArgumentException("Array or String join parameter is required but not satisfied (args.length <= index)");

                    if (commandParam.defaultValue().equals(commandParam.NULL)) {
                        arguments.add(null);
                        index = endIndex + 1;
                        continue;
                    }

                    // set it to the default value (when joinStart()) or to the default value split by a whitespace (when arrayStart())
                    joiningArray = commandParam.joinStart()
                            ? new String[]{commandParam.defaultValue()}
                            : commandParam.defaultValue().split(" ");
                } else {
                    joiningArray = Arrays.copyOfRange(args, index, endIndex + 1);
                }

                if (commandParam.required() && joiningArray.length == 0)
                    throw new ArgumentException("Array or String join parameter is required but not satisfied");

                if (ClassUtils.isAssignable(parameterType, String[].class) || ClassUtils.isAssignable(parameterType, String.class)) {
                    arguments.add(commandParam.joinStart() ? String.join(commandParam.joiner(), joiningArray) : joiningArray);
                } else {
                    Class<?> componentType = parameterType.getComponentType();
                    if (componentType != null) {
                        Object[] objectArray = (Object[]) Array.newInstance(componentType, joiningArray.length);
                        for (int i = 0; i < joiningArray.length; i++) {
                            String argument = joiningArray[i];
                            objectArray[i] = ObjectMapper.parseToObject(argument, componentType, commandParam);
                        }

                        arguments.add(objectArray);
                    } else {
                        arguments.add(ObjectMapper.parseToObject(String.join(commandParam.joiner(), joiningArray), parameterType, commandParam));
                    }
                }

                index = endIndex + 1;
                continue;
            }

            String arg;
            Object argument;
            if (args.length <= index) { // checks if the index is outside the argument array
                if (commandParam.required()) // as an own argument is required, the execution is ended here
                    throw new ArgumentException("Parameter is required but not satisfied (args.length <= index)");

                argument = null; // argument is set to 'null' as the argument is optional -> preparing for default value
            } else {
                arg = args[index];
                argument = ObjectMapper.parseToObject(arg, parameterType, commandParam); //parses the argument to the Object itself
            }

            if (argument == null) { // checks if the argument is null (argument parsing failed or argument is not given (see previous code block))
                if (commandParam.required())
                    throw new ArgumentException("Parameter is required but not satisfied (argument == null)");

                arg = commandParam.defaultValue().equals(commandParam.NULL) ? null : commandParam.defaultValue(); //the default argument
                argument = ObjectMapper.parseToObject(arg, parameterType, commandParam); //the default argument is parsed to the Object
            } else {
                index++; // argument was successfully read and parsed
            }

            arguments.add(argument);
        }

        return arguments.toArray();
    }

    private int getEndIndexOfArgumentArray(int startIndex, ListMultimap<Class<?>, CommandParam> commandParamMap) {
        ListMultimap<Class<?>, CommandParam> followingParams = extractFollowingParams(commandParamMap);

        int index = args.length - 1;
        if (followingParams.isEmpty()) return index; // array start is the last parameter

        List<Map.Entry<Class<?>, CommandParam>> entryList = new ArrayList<>(followingParams.entries()); //making #get available
        for (int i = entryList.size() - 1; i >= 0; i--) {
            if (index <= startIndex) return startIndex;

            Map.Entry<Class<?>, CommandParam> entry = entryList.get(i);

            Class<?> parameterType = entry.getKey();
            CommandParam param = entry.getValue();

            if (param.required()) {
                index--;
            } else {
                Object parsedArgument = ObjectMapper.parseToObject(args[index], parameterType, param);
                if (parsedArgument instanceof Boolean) {
                    if ((boolean) parsedArgument) {
                        index--;
                    }

                    continue;
                }

                if (parsedArgument != null) {
                    index--;
                }
            }
        }

        return index;
    }

    private ListMultimap<Class<?>, CommandParam> extractFollowingParams(ListMultimap<Class<?>, CommandParam> commandParamMap) {
        boolean arrayStart = false;
        ListMultimap<Class<?>, CommandParam> followingParams = LinkedListMultimap.create();
        for (Map.Entry<Class<?>, CommandParam> entry : commandParamMap.entries()) {
            Class<?> parameterType = entry.getKey();
            CommandParam commandParam = entry.getValue();

            if (arrayStart) {
                followingParams.put(parameterType, commandParam);
                continue;
            }

            if (commandParam.joinStart() || commandParam.arrayStart()) arrayStart = true;
        }

        return followingParams;
    }

    private ListMultimap<Class<?>, CommandParam> parseParameters() {
        Class<?>[] parameterTypes = onCommand.getParameterTypes();
        Annotation[][] parameterAnnotations = onCommand.getParameterAnnotations();

        ListMultimap<Class<?>, CommandParam> commandParamMap = LinkedListMultimap.create();
        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> parameterType = parameterTypes[i];
            Annotation[] annotations = parameterAnnotations[i];

            if (parameterType == UPlayer.class) continue;

            CommandParam commandParam = ReflectionUtil.getAnnotation(annotations, CommandParam.class);
            if (commandParam == null) commandParam = DefaultCommandParamSupplier.COMMAND_PARAM;

            commandParamMap.put(parameterType, commandParam);
        }

        return commandParamMap;
    }

}
