package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;

@Autonomous (name = "Autonomous 2.0", group = "Testing")
public class NEW_Autonomous extends OpMode {

    Robot r;
    StateMachine sm = new StateMachine();
    String skyCase = "left";
    boolean foundationSide, n  = false;
    double safeSpeed = .7;
    long wait = 0;
    boolean waitOS, confirmOS = false;

    @Override
    public void init() {
        r = Robot.getInstance();
        r.initialize(this);
        r.setServoPosition("srvFoundationLeft",.5);
        r.setServoPosition("srvFoundationRight", .5);
    }

    @Override
    public void init_loop(){
        sm.initializeMachine();
        if(sm.next_state_to_execute()) {
            telemetry.addData("Foundation Side", "A For Yes : B For No");
            if ((gamepad1.a ^ gamepad1.b) && !gamepad1.start) {
                foundationSide = gamepad1.a;
                n = true;
            }
            if (n && !gamepad1.a && !gamepad1.b) {
                n = false;
                telemetry.addData("Foundation Side", foundationSide+"");
                sm.incrementState();
            }
        }
        if(sm.next_state_to_execute()){
            telemetry.addData("Pause?","Dpad Down: -1 Second, Dpad Up: +1 Second");
            if(gamepad1.dpad_up && !waitOS){
                waitOS = true;
                wait+=1000;
            } else if (gamepad1.dpad_down && !waitOS){
                waitOS = true;
                wait-=1000;
            } else if (!gamepad1.dpad_up && !gamepad1.dpad_down){
                waitOS = false;
            } else if (gamepad1.a && !gamepad1.start && !confirmOS){
                confirmOS = true;
                sm.incrementState();
            } else if (!gamepad1.a){
                confirmOS = false;
            }
            telemetry.addData("Pause Amount:",wait);
        }

    }

    @Override
    public void start(){

    }

    @Override
    public void loop() {
        //TO make flip servo flip, use position .2-.3
        sm.initializeMachine();
        sm.pause(wait);

        /*
                             ___________     ___________     ___________
                            |           |   |           |   |           |
                            |     1     |   |     2     |   |     3     |
                            |___________|   |___________|   |___________|

                Objectives:

                           -Determine SkyStone Position with Vision Code
                           - POSITION 1
                                        -translate forward to the left
                                        -45 degree rotation
                                        -collect SkyStone
         */

//
        telemetry.addData("foundationSide", foundationSide+"");
        telemetry.addData("mtrLeftFront", r.getEncoderCounts("mtrFrontLeft"));
        telemetry.addData("mtrRightFront", r.getEncoderCounts("mtrRightFront"));
        telemetry.addData("mtrLeftBack", r.getEncoderCounts("mtrLeftBack"));
        telemetry.addData("mtrRightBack", r.getEncoderCounts("mtrRightBack"));
    }


    public void laps(double moreDistance){
        sm.translate(90, safeSpeed, 67+moreDistance);
        //Travel to Foundation and Place SkyStone
        sm.pause(500);
        sm.translate(-90, safeSpeed, 90+moreDistance);
        //Travel back for second stone
        sm.pause(500);
        //Grab second SkyStone
        sm.translate(90, safeSpeed, 90+moreDistance);
        //Travel to Foundation & Place Second SkyStone
        sm.pause(500);
        sm.translate(-90, safeSpeed, 40);
        //Park under bridge, ^ no need to change, 40 is same for all
    }
}
