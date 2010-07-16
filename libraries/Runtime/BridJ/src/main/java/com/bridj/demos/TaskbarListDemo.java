/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.bridj.demos;

import com.bridj.JNI;
import com.bridj.Pointer;
import com.bridj.cpp.com.COMRuntime;
import com.bridj.cpp.com.IUnknown;
import com.bridj.cpp.com.shell.IShellWindows;
import com.bridj.cpp.com.shell.ITaskbarList3;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 *
 * @author Olivier
 */
public class TaskbarListDemo extends JFrame implements ActionListener, ChangeListener {
    ITaskbarList3 list;

    int min = 0, max = 300, val = (min + max / 2);
    JSlider slider;

    Pointer<?> hwnd;

    public TaskbarListDemo() throws ClassNotFoundException {
        super("TaskbarList Demo (" + (JNI.is64Bits() ? "64 bits" : "32 bits") + ")");

        list = COMRuntime.newInstance(ITaskbarList3.class);
        
        getContentPane().add("Center", new JLabel("Hello Native Windows 7 World !"));
        Box box = Box.createVerticalBox();

        slider = new JSlider(min, max, val);
        slider.addChangeListener(this);
        box.add(slider);

        ButtonGroup group = new ButtonGroup();
        for (ITaskbarList3.TbpFlag state : ITaskbarList3.TbpFlag.values()) {
            JRadioButton cb = new JRadioButton(state.name());
            group.add(cb);
            cb.putClientProperty(ITaskbarList3.TbpFlag.class, state);
            cb.setSelected(state == ITaskbarList3.TbpFlag.TBPF_NORMAL);
            cb.addActionListener(this);
            box.add(cb);
        }
        getContentPane().add("South", box);

    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        list.Release();
    }

    public void setVisible(boolean visible) {
        super.setVisible(visible);

        long hwndVal = 0;//TODO com.sun.jna.Native.getComponentID(this);
        hwnd = Pointer.pointerToAddress(hwndVal);
        list.SetProgressValue((Pointer)hwnd, slider.getValue(), slider.getMaximum());
        
    }
    @Override
    public void stateChanged(ChangeEvent e) {
        list.SetProgressValue((Pointer)hwnd, slider.getValue(), slider.getMaximum());
    }
    @Override
    public void actionPerformed(ActionEvent e) {
        JRadioButton button = ((JRadioButton)e.getSource());
        if (button.isSelected()) {
            ITaskbarList3.TbpFlag flag = (ITaskbarList3.TbpFlag)button.getClientProperty(ITaskbarList3.TbpFlag.class);
            list.SetProgressValue((Pointer)hwnd, slider.getValue(), slider.getMaximum());
            list.SetProgressState((Pointer)hwnd, flag);
        }
    }

    public static void main(String[] args) throws Exception {
		try {
            
            TaskbarListDemo f = new TaskbarListDemo();
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.pack();
            f.setVisible(true);
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }
}
