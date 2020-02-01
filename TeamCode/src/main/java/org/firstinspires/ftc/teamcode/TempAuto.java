package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;

@Autonomous(name = "Current Auto")
public class TempAuto extends OpMode {
    StateMachine sm = new StateMachine();
    Robot r;

    @Override
    public void init() {
        r = Robot.getInstance();
        r.initialize(this);
    }

    @Override
    public void loop(){
        sm.initializeMachine();
        sm.pause(5000);
        sm.translate(180,0.5, 26);
    }
}
