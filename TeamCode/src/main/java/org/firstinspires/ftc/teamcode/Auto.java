package org.firstinspires.ftc.teamcode;

import android.os.Environment;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;

@Autonomous(name="AutoTest")
public class Auto extends OpMode {
    StateMachine drive = new StateMachine();
    StateMachine vision = new StateMachine();
    StateMachine lift = new StateMachine();
    Robot r = Robot.getInstance();

    File rFolder;
    ArrayList<String> fList = new ArrayList<>();
    boolean confirmOS = false, selOS = false;
    int fIndex = 0;

    String allianceColor = "blue"; //default
    String returnPath = "wall"; //default
    String apFoundationOrientation = "|"; //default
    boolean loadingSideStart = true; //default
    boolean apMoveFoundation = true; //default
    long wait = 0; //default
    int skyStones = 2; //default
    JSONObject settings;

    int[] vsData = {-1,-1};

    @Override
    public void init(){
        r.initialize(this);

        rFolder = new File(Environment.getExternalStorageDirectory() + "/JSONConfigs");
        if (!rFolder.exists())
            rFolder.mkdir();
        for (File f : rFolder.listFiles()) {
            if (f.getName().substring(f.getName().length() - 5).equals(".json"))
                fList.add(f.getName());
        }
    }

    @Override
    public void init_loop(){
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
                telemetry.addData("Alliance",  allianceColor);
                telemetry.addData("Side",loadingSideStart?"Loading Side":"Building Side");
                telemetry.addData("Wait Time", wait);
                telemetry.addData("Driving Path", returnPath);
                telemetry.addData("SkyStones",skyStones==3?"Both":skyStones==2?"Second":"First");
                telemetry.addData("Foundation", apMoveFoundation?"Yes":"No");
            } catch (Exception e) {
                e.printStackTrace();
                telemetry.addData("TRY CATCH ERROR", e.getCause());
            }

        }
    }

    @Override
    public void loop(){
        drive.initializeMachine();
        vision.initializeMachine();
        lift.initializeMachine();

        int[] temp = vision.getVisionData();
        vsData = temp == null ? vsData : temp;
        final int placement = allianceColor.equals("red") ? (5-vsData[0])%3 + 1 : vsData[0];
//        final int placement = 1;
        telemetry.addData("Placement: ", placement + "");
        telemetry.addData("xAverages: ", vsData[1]);
        vision.SetFlag(drive, "Vision Done");
        vision.SetFlag(lift, "Vision Done");

        drive.pause(wait);

        drive.WaitForFlag("Vision Done");

        if(allianceColor.equals("blue")) {

            if(loadingSideStart) { //BLUE & LOADING SIDE

                //lift.WaitForFlag("Vision Done");
                lift.setServoPower("srvRotator", r.rotatorOpen);
                lift.runToTarget("mtrLift", -100,0.6,true);
                lift.setServoPosition("srvClampLeft", r.leftOpen - 0.08 );
                lift.setServoPosition("srvClampRight", r.rightOpen);
                //lift.pause(500);


                drive.translate(90, 0.5, 26);
                drive.rotate(-90, 0.5);

                switch (placement) {
                    case 1:
                        drive.translate(-155, 0.5, 3.8);
                        break;
                    case 2:
                        drive.translate(85, 0.5, 7);
                        break;
                    case 3:
                        drive.translate(100, 0.5, 17);
                        break;
                }

                drive.SetFlag(lift, "In Position");

                lift.WaitForFlag("In Position");

                lift.runToTarget("mtrLift", 100, 0.6, true);
                lift.pause(250);
                lift.setServoPosition("srvClampLeft", r.leftClosed);
                lift.setServoPosition("srvClampRight", r.rightClosed);
                lift.pause(250);
                //lift.runToTarget("mtrLift",-50,0.6,true);
                lift.SetFlag(drive, "Grabbed");

                drive.WaitForFlag("Grabbed");

                drive.translate(0, 0.6, 5);

                switch (placement) {//Move to skyStone pos 1 even if we are at pos 2 || 3
                    case 2:
                        drive.translate(-85, 0.5, 8);
                        break;
                    case 3:
                        drive.translate(-85, 0.5, 16);
                        break;
                }
//hi
                if (returnPath.equals("wall")) {
                    drive.translate(-5, 0.5, 15);
                }
                drive.translate(-5,0.6,3);
                //Drive to Under Bridge
                drive.translate(-90, 0.5, 33);

                drive.setServoPosition("srvFoundRight", r.foundRightUp);
                drive.setServoPosition("srvFoundLeft", r.foundLeftUp);

                if (!apMoveFoundation) {
                    //If we are moving foundation 73
                    drive.SetFlag(lift, "Raise");
                    drive.translate(-90, 0.5, 55);

                    lift.WaitForFlag("Raise");
                    lift.runToTarget("mtrLift", -400, 0.6, true);

                    if (returnPath.equals("wall")) {
                        drive.translate(180, 0.5, 16);
                    }
                    //When we are not_wall path
                    drive.translate(180, 0.5, 10);
                    drive.SetFlag(lift, "Place");

                    lift.WaitForFlag("Place");
                    lift.runToTarget("mtrLift", 200, .6, true);
                    lift.setServoPosition("srvClampLeft", r.leftOpen);
                    lift.setServoPosition("srvClampRight", r.rightOpen);
                    //lift.pause(500);
                    //lift.runToTarget("mtrLift", -200, .6, true);
                    lift.SetFlag(drive, "Placed");



                    drive.WaitForFlag("Placed");
                    drive.translate(0, 0.5, 5);
                    drive.rotate(180, 0.5);
//                    drive.SetFlag(lift, "DropLift");

//                    lift.WaitForFlag("DropLift");
                    lift.setServoPosition("srvClampLeft", r.leftClosed-.08);
                    lift.setServoPosition("srvRotator", r.rotatorClosed);
                    lift.runToTarget("mtrLift", 200, 0.6, true);

                    drive.translate(0, 0.6, 5);

                    //Close Foundation Grabbers0
                    drive.setServoPosition("srvFoundRight", r.foundRightDown);
                    drive.setServoPosition("srvFoundLeft", r.foundLeftDown);

                    drive.translate(180, 0.6, 36);

                    //Open Foundation Grabbers

                    drive.setServoPosition("srvFoundRight", r.foundRightUp);
                    drive.setServoPosition("srvFoundLeft", r.foundLeftUp);

                    drive.translate(-90, 0.5, 35);
                    if (returnPath.equals("wall")) {
                        drive.translate(-90, 0.5, 30);
                    } else {
                        drive.translate(-50, 0.5, 50);
                    }

                }
            } else { //BLUE & BUILDING SIDE

                //line wheel up on outside of mat lines

                if(!apMoveFoundation) {

                    lift.setServoPosition("srvFoundRight", r.foundRightUp);
                    lift.setServoPosition("srvFoundLeft", r.foundLeftUp);

                    drive.translate(30, .5, 31.5);

                    drive.setServoPosition("srvFoundRight", r.foundRightDown);
                    drive.setServoPosition("srvFoundLeft", r.foundLeftDown);

                    drive.translate(180, .5, 29);

                    drive.setServoPosition("srvFoundRight", r.foundRightUp);
                    drive.setServoPosition("srvFoundLeft", r.foundLeftUp);
                }

                drive.translate(-90, 0.5, 35);
                if (returnPath.equals("wall")) {
                    drive.translate(-90, 0.5, 30);
                } else {
                    drive.translate(-35, 0.5, 45);
                }

            }

        } else {

            if(loadingSideStart){ //RED & LOADING SIDE

                lift.setServoPower("srvRotator", r.rotatorOpen);
                lift.runToTarget("mtrLift", -100,0.6,true);
                lift.setServoPosition("srvClampLeft", r.leftOpen - 0.08 );
                lift.setServoPosition("srvClampRight", r.rightOpen);
                //lift.pause(500);


                drive.translate(90, 0.5, 26);
                drive.rotate(-90, 0.5);

                switch (placement) {
                    case 1:
                        drive.translate(125, 0.5, 5.6);
                        break;
                    case 2:
                        drive.translate(-135, 0.5, 10);
                        break;
                    case 3:
                        drive.translate(-100, 0.5, 14);
                        break;
                }

                drive.SetFlag(lift, "In Position");

                lift.WaitForFlag("In Position");

                lift.runToTarget("mtrLift", 100, 0.6, true);
                lift.pause(200);
                lift.setServoPosition("srvClampLeft", r.leftClosed);
                lift.setServoPosition("srvClampRight", r.rightClosed);
                lift.pause(200);
                //lift.runToTarget("mtrLift",-75,0.6,true);
                lift.SetFlag(drive, "Grabbed");

                drive.WaitForFlag("Grabbed");

                drive.translate(0, 0.6, 6);
                switch (placement) {//Move to skyStone pos 1 even if we are at pos 2 || 3
                    case 2:
                        drive.translate(85, 0.5, 8);
                        break;
                    case 3:
                        drive.translate(85, 0.5, 16);
                        break;
                }
//hi
                if (returnPath.equals("wall")) {
                    drive.translate(5, 0.5, 14.75);
                }
                drive.translate(5,0.6,3);
                //Drive to Under Bridge
                drive.translate(90, 0.75, 33);
                drive.rotate(10,.5);

                drive.setServoPosition("srvFoundRight", r.foundRightUp);
                drive.setServoPosition("srvFoundLeft", r.foundLeftUp);

                if (!apMoveFoundation) {
                    //If we are moving foundation 73
                    drive.SetFlag(lift, "Raise");
                    drive.translate(90, 0.75, 55);
                    drive.rotate(22, .5);


                    lift.WaitForFlag("Raise");
                    lift.runToTarget("mtrLift", -400, 0.6, true);

                    if (returnPath.equals("wall")) {
                        drive.translate(180, 0.5, 16);
                    }
                    //When we are not_wall path
                    drive.translate(180, 0.5, 10);
                    drive.SetFlag(lift, "Place");

                    lift.WaitForFlag("Place");
                    lift.runToTarget("mtrLift", 200, .6, true);
                    lift.setServoPosition("srvClampLeft", r.leftOpen);
                    lift.setServoPosition("srvClampRight", r.rightOpen);
                    //lift.pause(500);
                    //lift.runToTarget("mtrLift", -200, .6, true);
                    lift.SetFlag(drive, "Placed");



                    drive.WaitForFlag("Placed");
                    drive.translate(0, 0.5, 5);
                    drive.rotate(180, 0.5);
//                    drive.SetFlag(lift, "DropLift");

//                    lift.WaitForFlag("DropLift");
                    lift.setServoPosition("srvClampLeft", r.leftClosed-.08);
                    lift.setServoPosition("srvRotator", r.rotatorClosed);
                    lift.runToTarget("mtrLift", 200, 0.6, true);

                    drive.translate(0, 0.6, 5);

                    //Close Foundation Grabbers0
                    drive.setServoPosition("srvFoundRight", r.foundRightDown);
                    drive.setServoPosition("srvFoundLeft", r.foundLeftDown);

                    drive.translate(180, 0.6, 39);

                    //Open Foundation Grabbers

                    drive.setServoPosition("srvFoundRight", r.foundRightUp);
                    drive.setServoPosition("srvFoundLeft", r.foundLeftUp);

                    drive.translate(90, 0.5, 35);
                    if (returnPath.equals("wall")) {
                        drive.translate(90, 0.5, 30);
                    } else {
                        drive.translate(50, 0.5, 50);
                    }

                }

            } else { //RED & BUILDING SIDE

                //line wheel up on outside of mat lines

                if(!apMoveFoundation) {

                    lift.setServoPosition("srvFoundRight", r.foundRightUp);
                    lift.setServoPosition("srvFoundLeft", r.foundLeftUp);

                    drive.translate(-30, .5, 31.5);

                    drive.setServoPosition("srvFoundRight", r.foundRightDown);
                    drive.setServoPosition("srvFoundLeft", r.foundLeftDown);

                    drive.translate(180, .5, 29);

                    drive.setServoPosition("srvFoundRight", r.foundRightUp);
                    drive.setServoPosition("srvFoundLeft", r.foundLeftUp);
                }

                drive.translate(90, 0.5, 35);
                if (returnPath.equals("wall")) {
                    drive.translate(90, 0.5, 30);
                } else {
                    drive.translate(35, 0.5, 39);
                }


            }

        }
        telemetry.addData("mtrLift Encoders: ",r.getEncoderCounts("mtrLift"));
        telemetry.addData("mtrLift RunMode: ",r.motors.get("mtrLift").getMode());
        telemetry.addData("mtrLeft Encoders: ",r.getEncoderCounts("mtrLeft"));
        telemetry.addData("mtrLift Encoders Target: ",r.motors.get("mtrLift").getTargetPosition());
    }
}
