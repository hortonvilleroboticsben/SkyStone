package org.firstinspires.ftc.teamcode;


import android.os.Environment;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.hardware.DcMotor;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.util.ArrayList;

@Autonomous (name = "Competition Autonomous", group = "Testing")
public class AutonomousComp extends OpMode {

    Robot r;
    StateMachine sm = new StateMachine();
    StateMachine vision = new StateMachine();
    StateMachine multitask = new StateMachine();
    boolean OSFound, OSColor = false;
    boolean waitOS, confirmOS = false;
    boolean selOS = false;
    Timer t = new Timer();

    JSONObject settings;
    File rFolder;
    ArrayList<String> fList = new ArrayList<>();
    int fIndex = 0;

    //    String allianceColor, returnPath, apFoundationOrientation = "No Change";
    String allianceColor = "blue"; //default
    String returnPath = "wall"; //default
    String apFoundationOrientation = "|"; //default
    boolean loadingSideStart = true; //default
    boolean apMoveFoundation = true; //default
    long wait = 0; //default
    int skyStones = 2; //default

    int[] vsData = null;

    @Override
    public void init() {
        r = Robot.getInstance();
        r.initialize(this);

        r.setServoPosition("srvFound", .85);

        rFolder = new File(Environment.getExternalStorageDirectory() + "/JSONConfigs");
        if (!rFolder.exists())
            rFolder.mkdir();
        for (File f : rFolder.listFiles()) {
            if (f.getName().substring(f.getName().length() - 5).equals(".json"))
                fList.add(f.getName());
        }

    }

    @Override
    public void init_loop() {
        sm.initializeMachine();
        if (!confirmOS) {
            if (fList.size() != 0) {
                telemetry.addData("Please select a file", fList.get(fIndex));

                if (gamepad1.dpad_down && !selOS) {
                    fIndex++;
                    selOS = true;
                }
                if (gamepad1.dpad_up && !selOS) {
                    fIndex--;
                    selOS = true;
                }
                if (!gamepad1.dpad_up && !gamepad1.dpad_down) selOS = false;

                fIndex = fIndex > fList.size() - 1 ? fList.size() - 1 : fIndex < 0 ? 0 : fIndex;

                if (gamepad1.a && !gamepad1.start && !gamepad2.start){
                    confirmOS = true;
                    telemetry.addData("File Selected",""+fList.get(fIndex));
                }
            } else {
                telemetry.addData("ERROR", "No files available.");
            }
        } else {
            File rPath = new File(rFolder, fList.get(fIndex));
            try {
                InputStream fin = new FileInputStream(rPath);
                byte[] raw_inp = new byte[fin.available()];
                fin.read(raw_inp);
                String om = new String(raw_inp);
                settings = new JSONObject(om);
                allianceColor = settings.getString("alliance");
                loadingSideStart = settings.getBoolean("loadingSideStart");
                wait = settings.getLong("pauseTime");
                returnPath = settings.getString("returnPath");
                skyStones = settings.getInt("skyStones");
                apMoveFoundation = settings.getBoolean("apMoveFoundation");
                apFoundationOrientation = settings.getString("apFoundationOrientation");
            } catch (Exception e) {
                e.printStackTrace();
                telemetry.addData("TRY CATCH ERROR", e.getCause());
            }

        }
    }

    @Override
    public void start() {
        sm.reset();
    }

    @Override
    public void loop() {
        sm.initializeMachine();
        vision.initializeMachine();
        multitask.initializeMachine();

// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ | VISION | ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        int[] temp = vision.getVisionData();
        vsData = temp == null ? vsData : temp;
        final int placement = allianceColor.equals("red") ? (5-vsData[0])%3 + 1 : vsData[0];
        telemetry.addData("Placement: ", placement + "");

        vision.SetFlag(sm, "Vision Done");

        sm.pause(wait);

        sm.WaitForFlag("Vision Done");

//#################################### | LOADING SIDE | #####################################

        if (loadingSideStart) {
            if (allianceColor.equals("blue")) {

// -------------------------------- < BLUE & LOADING SIDE > ----------------------------------------

                sm.translate(90, r.safeSpeed, 26);
                sm.rotate(-90, r.safeSpeed);

                sm.setServoPosition("srvClampLeft", r.leftOpen);
                sm.setServoPosition("srvClampRight", r.rightOpen);
                sm.setServoPower("srvRotator", r.rotatorOpen);

                switch (placement) {
                    case 1:
                        sm.translate(-140, r.safeSpeed, 8);
                        break;
                    case 2:
                        sm.translate(160, r.safeSpeed, 8);
                        break;
                    case 3:
                        sm.translate(110, r.safeSpeed, 12);
                        break;
                }// End of Switch Statement

// ................................. < CLAMP ONTO BLOCK > ..........................................

                sm.pause(500);

                sm.setServoPosition("srvClampRight", r.rightClosed);
                sm.setServoPosition("srvClampLeft", r.leftClosed);

                sm.pause(500);

                if (returnPath.equals("not_wall")) {
                    sm.translate(-87, r.safeSpeed, 73);
                    if (!apMoveFoundation) {
                        sm.translate(0, r.safeSpeed, 2);
                    }
                } else { //returnPath.equals("wall");
                    sm.translate(0, r.safeSpeed, 27);
                    sm.translate(-87, r.safeSpeed, 67);
                    sm.SetFlag(multitask,"Start");
                    //TODO: This flag isnt in the "not_wall"
                    if (!apMoveFoundation) {
                        sm.translate(180, r.safeSpeed, 20); //DRIVE TO FOUNDATION
                    }
                }

                multitask.WaitForFlag("Start");

// ===================================== LIFT TELEMETRY ================================

                telemetry.addData("flag", multitask.flag);
                telemetry.addData("state",multitask.state_in_progress);
                telemetry.addData("target", r.motors.get("mtrLift").getTargetPosition());
                telemetry.addData("pw", r.getPower("mtrLift"));
                telemetry.addData("rm", r.motors.get("mtrLift").getMode());
                telemetry.addData("enc",r.getEncoderCounts("mrtLift"));

// - - - - - - - - - - - - - - - - - - | LIFT | - - - - - - - - - - - - - - - - - - - - -

                if(multitask.next_state_to_execute()){
                    r.resetEncoder("mtrLift");
                    r.setTarget("mtrLift", -200);
                    r.setPower("mtrLift", 0.5);
                    r.setRunMode("mtrLift", DcMotor.RunMode.RUN_TO_POSITION);
                    multitask.incrementState();
                }

                sm.translate(180, r.safeSpeed-.1, 6);

                sm.setServoPosition("srvClampLeft", r.leftOpen);
                sm.setServoPosition("srvClampRight", r.rightOpen);

                if(multitask.next_state_to_execute()){
                    if(r.hasMotorEncoderReached("mtrLift", -200)){
                        r.setPower("mtrLift", 0);
                        multitask.incrementState();
                    }
                }

                multitask.SetFlag(sm, "Done");

                sm.WaitForFlag("Done");

                sm.translate(0, r.safeSpeed, 2.5);

                //get foundation

                if (!apMoveFoundation) {
                    sm.rotate(-95, r.safeSpeed);
                    sm.translate(-90,r.safeSpeed,2);
                    sm.translate(0, r.safeSpeed, 15.5);
                    sm.setServoPower("srvFound", r.foundDown);
                    sm.pause(500);
                    sm.translate(90, .75, 50);
                    sm.setServoPower("srvFound", r.foundUp);
                }

                if(multitask.next_state_to_execute()){
                    r.resetEncoder("mtrLift");
                    r.setTarget("mtrLift", 0);
                    r.setPower("mtrLift", 0.5);
                    r.setRunMode("mtrLift", DcMotor.RunMode.RUN_TO_POSITION);
                    multitask.incrementState();
                }

                if(multitask.next_state_to_execute()){
                    if(r.hasMotorEncoderReached("mtrLift", 0)){
                        r.setPower("mtrLift", 0);
                        multitask.incrementState();
                    }
                }

                multitask.SetFlag(sm, "Done");

                sm.WaitForFlag("Done");

// - - - - - - - - - - - - - - - - - - - | Park Under Bridge | - - - - - - - - - - - - - - - -
                if (returnPath.equals("wall")) {

                    sm.translate(175, r.safeSpeed, 52.5);
                    sm.setServoPosition("srvClampRight", r.rightOpen);
                    sm.setServoPosition("srvClampLeft", r.leftOpen);

                } else if (returnPath.equals("not_wall")) {

                    sm.translate(175, r.safeSpeed, 52.5);
//                    sm.setServoPosition("srvClampRight", .1);
//                    sm.setServoPosition("srvClampLeft", .3);
                    sm.translate(-90, r.safeSpeed, 24);
                    sm.translate(180, r.safeSpeed, 5);
                    sm.setServoPosition("srvClampRight", r.rightClosed);
                    sm.setServoPosition("srvClampLeft", r.leftClosed);

                }

/*
                if (skyStones == 2) { //GOING FOR SECOND SKYSTONE
                    switch (placement) {
                        case 1:
                            sm.translate(175, r.safeSpeed, 52.5);
                            if (returnPath.equals("wall")) {
                                sm.translate(-90, r.safeSpeed, 5);
                                sm.rotate(90, r.safeSpeed);
                                sm.translate(180, r.safeSpeed, 25);
                            } else {
                                //Get enough space to rotate into the stone
                            }
                            break;
                        case 2:

                            break;

                        case 3:

                            break;
                    }
                }
*/

            } else { //ALLIANCE COLOR === RED, LOADING SIDE

                //RED & LOADING SIDE

                //TODO: Edit these values

                sm.translate(90, r.safeSpeed, 26);
                sm.rotate(-90, r.safeSpeed);

                sm.setServoPosition("srvClampLeft", r.leftOpen);
                sm.setServoPosition("srvClampRight", r.rightOpen);
                sm.setServoPower("srvRotator", r.rotatorOpen);

                switch (placement) {
                    case 1:
                        sm.translate(140, r.safeSpeed, 8);
                        break;
                    case 2:
                        sm.translate(160, r.safeSpeed, 8);
                        break;
                    case 3:
                        sm.translate(110, r.safeSpeed, 12);
                        break;
                }// End of Switch Statement

                sm.setServoPosition("srvClampLeft", r.leftClosed);
                sm.setServoPosition("srvClampRight", r.rightClosed);


                if (returnPath.equals("not_wall")) {
                    sm.translate(82, r.safeSpeed, 73); //90 is for good

                    if (!apMoveFoundation) {
                        sm.translate(180, r.safeSpeed, 6); //DRIVE TO FOUNDATION
                    }
                } else { //returnPath.equals("wall");
                    sm.translate(0, r.safeSpeed, 25);
                    sm.translate(82, r.safeSpeed, 73); //90 is for good

                    if (!apMoveFoundation) {
                        sm.translate(180, r.safeSpeed, 20); //DRIVE TO FOUNDATION
                    }

                }

                sm.SetFlag(multitask, "READY");

// - - - - - - - - - - - - - - - - - - - - - - | LIFT | - - - - - - - - - - - - - - - - - - - - - -

                multitask.WaitForFlag("READY");

                if(multitask.next_state_to_execute()){
                    r.resetEncoder("mtrLift");
                    r.setTarget("mtrLift", -200);
                    r.setPower("mtrLift", 0.5);
                    r.setRunMode("mtrLift", DcMotor.RunMode.RUN_TO_POSITION);
                    multitask.incrementState();
                }

                if(multitask.next_state_to_execute()){
                    if(r.hasMotorEncoderReached("mtrLift", -200)){
                        r.setPower("mtrLift", -0);
                        multitask.incrementState();
                    }
                }

                multitask.SetFlag(sm, "Done");

                sm.WaitForFlag("Done");

                sm.translate(0, r.safeSpeed-.1, 2.5);

                sm.translate(180, r.safeSpeed-.1, 2);

                sm.setServoPosition("srvClampRight", r.rightOpen);
                sm.setServoPosition("srvClampLeft", r.leftOpen);

                sm.pause(500);

// - - - - - - - - - - - - - - - - - - - | GRAB FOUNDATION | - - - - - - - - - - - - - - - - - - -

                if (!apMoveFoundation) {
                    sm.rotate(-90, r.safeSpeed);
                    sm.translate(180, r.safeSpeed, 15.5);
                    sm.setServoPosition("srvFound", r.foundDown);
                    sm.pause(500);
                    sm.translate(90, .75, 50);
                    sm.setServoPosition("srvFound", r.foundUp);
                }

// - - - - - - - - - - - - - - - - - - - | LOWER LIFT | - - - - - - - - - - - - - - - - - - - - - -

                if(multitask.next_state_to_execute()){
                    r.resetEncoder("mtrLift");
                    r.setTarget("mtrLift", 0);
                    r.setPower("mtrLift", 0.5);
                    r.setRunMode("mtrLift", DcMotor.RunMode.RUN_TO_POSITION);
                    multitask.incrementState();
                }

                if(multitask.next_state_to_execute()){
                    if(r.hasMotorEncoderReached("mtrLift", 0)){
                        r.setPower("mtrLift", 0);
                        multitask.incrementState();
                    }
                }

                multitask.SetFlag(sm, "Done");

                sm.WaitForFlag("Done");

// *************************************** | PARK UNDER BRIDGE | ***********************************

                if (returnPath.equals("wall")) {
                    sm.translate(0, r.safeSpeed, 52.5);
                } else if (returnPath.equals("not_wall")) {
                    sm.translate(0, r.safeSpeed, 45);
                    sm.translate(-90, r.safeSpeed, 24);
                    sm.translate(0, r.safeSpeed, 2);
                }

                sm.setServoPosition("srvClampLeft", r.leftOpen);
                sm.setServoPosition("srvClampRight", r.rightOpen);

//                if (skyStones == 2) { //GOING FOR SECOND SKYSTONE
//                    switch (placement) {
//                        case 1:
//                            sm.translate(175, r.safeSpeed, 52.5);
//                            if (returnPath.equals("wall")) {
//                                sm.translate(-90, r.safeSpeed, 5);
//                                sm.rotate(90, r.safeSpeed);
//                                sm.translate(180, r.safeSpeed, 25);
//                            } else {
//                                //Get enough space to rotate into the stone
//                            }
//                            break;
//                        case 2:
//
//                            break;
//
//                        case 3:
//
//                            break;
//                    }
//                }

            }

        } else {

//########################################## | BUILDING SIDE | #######################################

            if (allianceColor.equals("blue")) {
//...................................<BLUE & BUILDING SIDE>.................................

                if (!apMoveFoundation) { // MOVE THE FOUNDATION
                    sm.translate(-90, r.safeSpeed, 28);
                    sm.setServoPosition("srvFound", r.foundDown);
                    sm.pause(500);
                    sm.translate(90, .75, 50);
                    sm.setServoPosition("srvFound", r.foundUp);

                }

//- - - - - - - - - - - - - - - - - - | Park Under Bridge | - - - - - - - - - - - - - - - - -

                if (returnPath.equals("not_wall")) {
                    sm.translate(175, r.safeSpeed, 25);
                    sm.translate(-135, r.safeSpeed, 25);
                    sm.translate(180, r.safeSpeed, 9);

                } else { //returnPath.equals("wall");

                    sm.translate(175, r.safeSpeed, 50.5);
                }

            } else { // ALLIANCE COLOR === RED

//........................................<RED & BUILDING SIDE>..............................

                if (!apMoveFoundation) { // MOVE THE FOUNDATION
                    sm.translate(-90, r.safeSpeed, 28);
                    sm.setServoPosition("srvFound", r.foundDown);
                    sm.pause(500);
                    sm.translate(90, .75, 50);
                    sm.setServoPosition("srvFound", r.foundUp);

                }

//- - - - - - - - - - - - - - - - - - | Park Under Bridge | - - - - - - - - - - - - - - - - -

                if (returnPath.equals("not_wall")) {
                    sm.translate(175, r.safeSpeed, 25);
                    sm.translate(-135, r.safeSpeed, 25);
                    sm.translate(180, r.safeSpeed, 9);

                } else { //returnPath.equals("wall");
                    sm.translate(175, r.safeSpeed, 50.5);
                }

                sm.setServoPosition("srvClampLeft", r.leftOpen);
                sm.setServoPosition("srvClampRight", r.rightOpen);

//......................................<COLOR SENSORS>.....................................

//                    if have working color sensors

//              if(sm.next_state_to_execute()) {
//                   int frontBlue = r.getColorValue("colorFront", "blue");
//                   int backBlue = r.getColorValue("colorBack", "blue");
//                   if(frontBlue<11){
//                       //WheelSetL is static, might cause problem
//              	     r.setPower(Robot.wheelSet1[0],.2);
//                           r.setPower(Robot.wheelSet2[0],-.2);
//                   }else{
//                           r.setPower(Robot.wheelSet1[0],-.3);
//                           r.setPower(Robot.wheelSet2[0],.3);
//                   }
//                   if(backBlue<11){
//                           r.setPower(Robot.wheelSet1[1],.2);
//                           r.setPower(Robot.wheelSet2[1],-.2);
//                   }else{
//                           r.setPower(Robot.wheelSet1[1],-.3);
//                           r.setPower(Robot.wheelSet2[1],.3);
//                   }
//                   if(frontBlue >= 11 && backBlue >= 11){
//                            r.setDrivePower(0,0);
//                            sm.incrementState();
//                    }
//               }

            }

//.......................................<TELEMETRY>..........................................

            telemetry.addData("mtrLeftFront", r.getEncoderCounts("mtrFrontLeft"));
            telemetry.addData("mtrRightFront", r.getEncoderCounts("mtrRightFront"));
            telemetry.addData("mtrLeftBack", r.getEncoderCounts("mtrLeftBack"));
            telemetry.addData("mtrRightBack", r.getEncoderCounts("mtrRightBack"));

            telemetry.addData("RED Front", r.getColorValue("colorFront", "red"));
            telemetry.addData("BLUE", r.getColorValue("colorFront", "blue"));
            telemetry.addData("GREEN", r.getColorValue("colorFront", "green"));

            telemetry.addData("RED Back", r.getColorValue("colorBack", "red"));
            telemetry.addData("BLUE", r.getColorValue("colorBack", "blue"));
            telemetry.addData("GREEN", r.getColorValue("colorBack", "green"));
        }
    }
}