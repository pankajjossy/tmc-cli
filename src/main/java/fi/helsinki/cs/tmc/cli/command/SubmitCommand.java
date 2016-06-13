package fi.helsinki.cs.tmc.cli.command;

import fi.helsinki.cs.tmc.cli.Application;
import fi.helsinki.cs.tmc.cli.command.core.AbstractCommand;
import fi.helsinki.cs.tmc.cli.command.core.Command;
import fi.helsinki.cs.tmc.cli.io.Color;
import fi.helsinki.cs.tmc.cli.io.Io;
import fi.helsinki.cs.tmc.cli.io.ResultPrinter;
import fi.helsinki.cs.tmc.cli.tmcstuff.CourseInfo;
import fi.helsinki.cs.tmc.cli.tmcstuff.CourseInfoIo;
import fi.helsinki.cs.tmc.cli.tmcstuff.TmcUtil;
import fi.helsinki.cs.tmc.cli.tmcstuff.WorkDir;
import fi.helsinki.cs.tmc.core.TmcCore;
import fi.helsinki.cs.tmc.core.domain.Course;
import fi.helsinki.cs.tmc.core.domain.submission.SubmissionResult;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;

@Command(name = "submit", desc = "Submit exercises")
public class SubmitCommand extends AbstractCommand {

    private static final Logger logger = LoggerFactory.getLogger(SubmitCommand.class);

    private Application app;
    private Io io;
    private boolean showAll;
    private boolean showDetails;

    public SubmitCommand(Application app) {
        this.app = app;
    }

    @Override
    public void getOptions(Options options) {
        options.addOption("a", "all", false, "Show all test results");
        options.addOption("d", "details", false, "Show detailed error message");
    }

    @Override
    public void run(CommandLine args, Io io) {
        this.io = io;
        TmcCore core;
        WorkDir dirUtil;

        String[] exerciseNames = parseArgs(args);

        if (exerciseNames == null) {
            return;
        }
        dirUtil = new WorkDir();
        core = this.app.getTmcCore();
        if (core == null) {
            return;
        }
        Path courseDir = dirUtil.getCourseDirectory();

        if (courseDir == null) {
            System.out.println("Not a course directory");
            return;
        }

        CourseInfo info = CourseInfoIo.load(dirUtil.getConfigFile());
        String courseName = info.getCourseName();
        Course course = TmcUtil.findCourse(core, courseName);

        List<String> exercises;
        exercises = dirUtil.getExerciseNames(exerciseNames);
        
        if (exercises.isEmpty()) {
            io.println("You have to be in the exercise root directory to submit."
                    + " (This is a known problem.)");
            return;
        }

        ResultPrinter resultPrinter = new ResultPrinter(io, this.showDetails, this.showAll);
        SubmissionResult result;

        for (String exerciseName : exercises) {
            io.println(Color.colorString("Submitting: " + exerciseName, Color.ANSI_YELLOW));
            result = TmcUtil.submitExercise(core, course, exerciseName);
            resultPrinter.printSubmissionResult(result);
            io.println("");
        }
    }

    private String[] parseArgs(CommandLine args) {
        this.showAll = args.hasOption("a");
        this.showDetails = args.hasOption("d");
        return args.getArgs();
    }
}
