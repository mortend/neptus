/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
 * Laboratório de Sistemas e Tecnologia Subaquática (LSTS)
 * All rights reserved.
 * Rua Dr. Roberto Frias s/n, sala I203, 4200-465 Porto, Portugal
 *
 * This file is part of Neptus, Command and Control Framework.
 *
 * Commercial Licence Usage
 * Licencees holding valid commercial Neptus licences may use this file
 * in accordance with the commercial licence agreement provided with the
 * Software or, alternatively, in accordance with the terms contained in a
 * written agreement between you and Universidade do Porto. For licensing
 * terms, conditions, and further information contact lsts@fe.up.pt.
 *
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: zp
 * Jun 30, 2013
 */
package pt.up.fe.dceg.neptus.plugins.sunfish;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;

import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.comm.iridium.ActivateSubscription;
import pt.up.fe.dceg.neptus.comm.iridium.DeactivateSubscription;
import pt.up.fe.dceg.neptus.comm.iridium.DesiredAssetPosition;
import pt.up.fe.dceg.neptus.comm.iridium.DeviceUpdate;
import pt.up.fe.dceg.neptus.comm.iridium.IridiumCommand;
import pt.up.fe.dceg.neptus.comm.iridium.IridiumFacade;
import pt.up.fe.dceg.neptus.comm.iridium.IridiumMessage;
import pt.up.fe.dceg.neptus.comm.iridium.IridiumMessageListener;
import pt.up.fe.dceg.neptus.comm.iridium.TargetAssetPosition;
import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.gui.PropertiesEditor;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.imc.RemoteSensorInfo;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.SimpleRendererInteraction;
import pt.up.fe.dceg.neptus.renderer2d.Renderer2DPainter;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.vehicle.VehicleType;
import pt.up.fe.dceg.neptus.types.vehicle.VehiclesHolder;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcMsgManager;

import com.google.common.eventbus.Subscribe;

/**
 * @author zp
 *
 */
@PluginDescription(name="Iridium Communications Plug-in")
public class IridiumComms extends SimpleRendererInteraction implements Renderer2DPainter, IridiumMessageListener {

    private static final long serialVersionUID = -8535642303286049869L;
    protected long lastMessageReceivedTime = System.currentTimeMillis() - 3600000;
    protected LinkedHashMap<String, RemoteSensorInfo> sensorData = new LinkedHashMap<>();
    protected Image spot, desired, target;
    
    @Override
    public boolean isExclusive() {
        return true;
    }
    
    public void loadImages() {
        //TODO
    }
    
    private void sendIridiumCommand() {
        String cmd = JOptionPane.showInputDialog(getConsole(), I18n.textf("Enter command to be sent to %vehicle", getMainVehicleId()));
        if (cmd == null || cmd.isEmpty())
            return;
        
        IridiumCommand command = new IridiumCommand();
        command.setCommand(cmd);
        
        VehicleType vt = VehiclesHolder.getVehicleById(getMainVehicleId());
        if (vt == null) {
            GuiUtils.errorMessage(getConsole(), "Send Iridium Command", "Could not calculate destination's IMC identifier");
            return;
        }
        command.setDestination(vt.getImcId().intValue());
        command.setSource(ImcMsgManager.getManager().getLocalId().intValue());
        try {
            IridiumFacade.getInstance().sendMessage(command);    
        }
        catch (Exception e) {
            GuiUtils.errorMessage(getConsole(), e);
        }
    }
    
    private void setWaveGliderTargetPosition(LocationType loc) {
        TargetAssetPosition pos = new TargetAssetPosition();
        pos.setLocation(loc);
        pos.setDestination(0);
        pos.setSource(ImcMsgManager.getManager().getLocalId().intValue());
        try {
            IridiumFacade.getInstance().sendMessage(pos);    
        }
        catch (Exception e) {
            GuiUtils.errorMessage(getConsole(), e);
        }
    }

    private void setWaveGliderDesiredPosition(LocationType loc) {
        DesiredAssetPosition pos = new DesiredAssetPosition();
        pos.setLocation(loc);
        pos.setDestination(0);
        pos.setSource(ImcMsgManager.getManager().getLocalId().intValue());
        try {
            IridiumFacade.getInstance().sendMessage(pos);    
        }
        catch (Exception e) {
            GuiUtils.errorMessage(getConsole(), e);
        }
    }
    
    private void startIridiumSimulation() {
        Thread t = new Thread("Iridium Simulation") {
            public void run() {
                while(true) {
                    int rnd = (int) Math.round(Math.random() * 7) + 2001;
                    switch (rnd) {
                        case 2001:
                            DeviceUpdate m = new DeviceUpdate();
                            DeviceUpdate.Position pos = new DeviceUpdate.Position();
                            pos.id = VehiclesHolder.getVehicleById("lauv-seacon-2").getImcId().intValue();
                            pos.latitude = LocationType.FEUP.getLatitudeAsDoubleValue();
                            pos.timestamp = System.currentTimeMillis() / 1000;
                            pos.longitude = LocationType.FEUP.getLongitudeAsDoubleValue();
                            m.getPositions().put(pos.id, pos);
                            m.setSource(ImcMsgManager.getManager().getLocalId().intValue());
                            m.setDestination(0);
                            messageReceived(m);
                            break;
                        case 2003:
                            ActivateSubscription act = new ActivateSubscription();                            
                            act.setSource(ImcMsgManager.getManager().getLocalId().intValue());
                            act.setDestination(0);
                            messageReceived(act);
                            break;
                        case 2004:
                            DeactivateSubscription deact = new DeactivateSubscription();                            
                            deact.setSource(ImcMsgManager.getManager().getLocalId().intValue());
                            deact.setDestination(0);
                            messageReceived(deact);
                            break;
                        case 2005:
                            IridiumCommand cmd = new IridiumCommand();
                            cmd.setCommand("This is a test command");
                            cmd.setSource(ImcMsgManager.getManager().getLocalId().intValue());
                            cmd.setDestination(0);
                            messageReceived(cmd);
                            break;
                        default:
                            
                            break;
                    }
                    try {
                        Thread.sleep(10000);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        t.start();
        
    }
    
    @Override
    public void mouseClicked(MouseEvent event, StateRenderer2D source) {
        if (event.getButton() != MouseEvent.BUTTON3)
            super.mouseClicked(event,source);
        
        final LocationType loc = source.getRealWorldLocation(event.getPoint());
        loc.convertToAbsoluteLatLonDepth();
        
        JPopupMenu popup = new JPopupMenu();
        popup.add(I18n.textf("Send %vehicle a command via Iridium", getMainVehicleId())).addActionListener(new ActionListener() {            
            public void actionPerformed(ActionEvent e) {
                sendIridiumCommand();                
            }
        });

        popup.add("Set this as actual wave glider target").addActionListener(new ActionListener() {            
            public void actionPerformed(ActionEvent e) {
                setWaveGliderTargetPosition(loc);
            }
        });
        
        popup.add("Set this as desired wave glider target").addActionListener(new ActionListener() {            
            public void actionPerformed(ActionEvent e) {
                setWaveGliderDesiredPosition(loc);
            }
        });
        
        popup.add("Start iridium simulation").addActionListener(new ActionListener() {            
            public void actionPerformed(ActionEvent e) {
                startIridiumSimulation();
            }
        });
        
        popup.addSeparator();
        
        popup.add("Settings").addActionListener(new ActionListener() {            
            public void actionPerformed(ActionEvent e) {
                PropertiesEditor.editProperties(IridiumComms.this, getConsole(), true);
            }
        });
        
        popup.show(source, event.getX(), event.getY());
    }
    
    @Subscribe
    public void on(RemoteSensorInfo msg) {
        NeptusLog.pub().info("Got device update from "+msg.getId()+": "+sensorData);        
        sensorData.put(msg.getId(), msg);
    }
    
    public IridiumComms(ConsoleLayout console) {
        super(console);
    }
    
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        for (RemoteSensorInfo sinfo : sensorData.values()) {
            if (sinfo.getId().startsWith("DP_")) {
                
            }
            else if (sinfo.getId().startsWith("TP_")) {
                
            }
            else if (sinfo.getId().startsWith("spot") || sinfo.getId().startsWith("SPOT")) {
                
            }
        }
    }
    
    @Subscribe
    public void on(DesiredAssetPosition desiredPos) {
        System.out.println(desiredPos);
    }
    
    @Subscribe
    public void on(TargetAssetPosition targetPos) {
        System.out.println(targetPos);
    }
    
    @Subscribe
    public void on(DeviceUpdate devUpdate) {
        System.out.println(devUpdate);
    }
    
    @Override
    public void messageReceived(IridiumMessage msg) {
        NeptusLog.pub().info("Iridium message received asynchronously: "+msg);
        getConsole().post(msg);
        for (IMCMessage m : msg.asImc())
            getConsole().post(m);
    }

    @Override
    public void initSubPanel() {
        IridiumFacade.getInstance().addListener(this);     
    }

    @Override
    public void cleanSubPanel() {
        IridiumFacade.getInstance().removeListener(this);
    }
}
