package fi.helsinki.cs.tmc.cli.command;

import fi.helsinki.cs.tmc.cli.Application;
import fi.helsinki.cs.tmc.cli.command.core.Command;
import fi.helsinki.cs.tmc.cli.command.core.CommandInterface;
import fi.helsinki.cs.tmc.cli.io.Io;

import fi.helsinki.cs.tmc.cli.io.TmcCliProgressObserver;
import fi.helsinki.cs.tmc.cli.tmcstuff.CourseInfo;
import fi.helsinki.cs.tmc.cli.tmcstuff.CourseInfoIo;
import fi.helsinki.cs.tmc.cli.tmcstuff.DirectoryUtil;
import fi.helsinki.cs.tmc.cli.tmcstuff.ExternalsUtil;

import fi.helsinki.cs.tmc.core.TmcCore;
import fi.helsinki.cs.tmc.core.domain.Exercise;
import fi.helsinki.cs.tmc.core.exceptions.FailedHttpResponseException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;
import java.util.concurrent.Callable;

@Command(name = "paste", desc = "Submit exercise to pastebin")
public class PasteCommand implements CommandInterface {
    private static final Logger logger = LoggerFactory.getLogger(SubmitCommand.class);
    private final Options options;

    private Application app;
    private Io io;

    public PasteCommand(Application app) {
        this.app = app;
        this.options = new Options();
        this.options.addOption("m", "message", true, "Add a message to your paste");
        this.options.addOption("o", "open", false, "Open the link to your paste in a web browser");
    }

    @Override
    public void run(String[] args, Io io) {
        this.io = io;
        CommandLine line = parseData(args);
        String message = line.getOptionValue("m");
        if (message == null) {
            message = ExternalsUtil.getUserEditedMessage(
                    "\n"
                    + "#Write a message for your paste. "
                    + "Leaving the message empty will abort the paste.\n"
                    + "#Lines beginning with # are comments and will be ignored.",
                    "tmc_paste_message.txt",
                    true);
        }
        io.println(message);
        if (message == null || message.length() == 0) {
            io.println("Paste message empty, aborting");
            return;
        }
        TmcCore core = this.app.getTmcCore();
        DirectoryUtil dirutil = new DirectoryUtil();
        List<String> exerciseNames = dirutil.getExerciseNames(line.getArgs());
        if (exerciseNames.size() != 0) {
            io.println(
                    "No exercise specified. Please use this command from an exercise directory or"
                    + "\npass the name of the exercise as an argument.");
            return;
        }

        String exerciseName = exerciseNames.get(0);
        CourseInfoIo infoio = new CourseInfoIo(dirutil.getConfigFile());
        CourseInfo courseinfo = infoio.load();
        Exercise exercise = null;
        Callable<URI> callable = core.pasteWithComment(
                new TmcCliProgressObserver(), exercise, message);
        URI uri;
        try {
            uri = callable.call();
        } catch (Exception e) {
            Throwable cause = e.getCause();
            if (cause instanceof FailedHttpResponseException) {
                FailedHttpResponseException httpEx
                        = (FailedHttpResponseException) cause;
                if (httpEx.getStatusCode() == 401) {
                    io.println("Authentication failed. Are you logged in?");
                }
                return;
            }
            logger.error("Unable to connect to server", e);
            io.println("Unable to connect to server");
            return;
        }
        io.println("Paste sent for exercise " + exercise.getName());
        io.println(uri.toString());
        if (options.hasOption("open")) {
            ExternalsUtil.openInBrowser(uri);
        }
    }

    private CommandLine parseData(String[] args) {
        GnuParser parser = new GnuParser();
        try {
            return parser.parse(options, args);
        } catch (ParseException e) {
            logger.warn("Unable to parse message.", e);
        }
        return null;
    }


}
