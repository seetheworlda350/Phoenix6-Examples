package frc.robot.commands;

import java.util.function.DoubleSupplier;

import com.ctre.phoenixpro.StatusSignalValue;

import edu.wpi.first.wpilibj.Notifier;
import edu.wpi.first.wpilibj2.command.CommandBase;
import frc.robot.subsystems.DriveSubsystem;

public class DriveStraightCommand extends CommandBase {
    final double MAX_UPDATE_PERIOD = 0.05; // Wait up to 50ms
    DoubleSupplier m_throttle;
    DriveSubsystem m_drivebase;
    StatusSignalValue<Double> m_yawGetter;
    double m_holdYaw = 0;

    /**
     * We do the calculation and updates in a separate thread to make use
     * of the WaitForUpdate from StatusSignalValue. This allows us to
     * react immediately to new data, instead of reacting to potentially
     * old data
     */
    Notifier m_driveStraightThread;

    public DriveStraightCommand(DriveSubsystem drivebase, DoubleSupplier throttle) {
        m_throttle = throttle;
        m_drivebase = drivebase;
        m_yawGetter = m_drivebase.getYaw();
        m_driveStraightThread = new Notifier(()->driveStraightExecution());
        addRequirements(drivebase);
    }

    /**
     * This is the threaded context method that gets called as fast as possible.
     * The timing is determined by how fast the Pigeon2 reports its yaw, up to MAX_UPDATE_PERIOD
     */
    private void driveStraightExecution()
    {
        /* Get our current yaw and find the error from the yaw we want to hold */
        double err = m_holdYaw - m_yawGetter.waitForUpdate(MAX_UPDATE_PERIOD).getValue();
        /* Simple P-loop, where 100 degrees off corresponds to 100% output */
        double correction = err * 0.01;
        /* And apply it to the arcade drive */
        m_drivebase.arcadeDrive(m_throttle.getAsDouble(), correction);
    }

    @Override
    public void initialize()
    {
        /* On initialize, latch the current yaw and begin correction */
        m_holdYaw = m_yawGetter.waitForUpdate(MAX_UPDATE_PERIOD).getValue();
        /* Update as fast as possible, the waitForUpdate will manage the loop period */
        m_driveStraightThread.startPeriodic(0);
    }

    /* No need for an execute, as our thread will execute automatically */

    @Override
    public void end(boolean isInterrupted)
    {
        /* Stop the notifier */
        m_driveStraightThread.stop();
    }

}