package de.fuzzlemann.ucutils.base.command.execution;

import de.fuzzlemann.ucutils.base.command.CommandParam;

/**
 * @author Fuzzlemann
 */
class DefaultCommandParamSupplier {

    static final CommandParam COMMAND_PARAM;

    static {
        CommandParam commandParam;
        try {
            commandParam = (CommandParam) DefaultCommandParamSupplier.class.getDeclaredMethod("commandParamSupplier", String.class).getParameterAnnotations()[0][0];
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e); //should not happen
        }

        COMMAND_PARAM = commandParam;
    }

    private static void commandParamSupplier(@CommandParam String commandParam) {
    }
}
