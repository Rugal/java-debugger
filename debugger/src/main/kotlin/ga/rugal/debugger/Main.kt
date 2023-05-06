package ga.rugal.debugger

import java.io.IOException
import java.util.TimeZone
import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.gui2.BasicWindow
import com.googlecode.lanterna.gui2.Button
import com.googlecode.lanterna.gui2.ComboBox
import com.googlecode.lanterna.gui2.Direction
import com.googlecode.lanterna.gui2.EmptySpace
import com.googlecode.lanterna.gui2.GridLayout
import com.googlecode.lanterna.gui2.Label
import com.googlecode.lanterna.gui2.MultiWindowTextGUI
import com.googlecode.lanterna.gui2.Panel
import com.googlecode.lanterna.gui2.Separator
import com.googlecode.lanterna.gui2.TextBox
import com.googlecode.lanterna.gui2.Window
import com.googlecode.lanterna.gui2.WindowBasedTextGUI
import com.googlecode.lanterna.gui2.dialogs.MessageDialog
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton
import com.googlecode.lanterna.screen.Screen
import com.googlecode.lanterna.terminal.DefaultTerminalFactory
import com.sun.jdi.AbsentInformationException
import com.sun.jdi.Bootstrap
import com.sun.jdi.IncompatibleThreadStateException
import com.sun.jdi.StackFrame
import com.sun.jdi.VMDisconnectedException
import com.sun.jdi.VirtualMachine
import com.sun.jdi.event.AccessWatchpointEvent
import com.sun.jdi.event.BreakpointEvent
import com.sun.jdi.event.ClassPrepareEvent
import com.sun.jdi.event.ClassUnloadEvent
import com.sun.jdi.event.ExceptionEvent
import com.sun.jdi.event.LocatableEvent
import com.sun.jdi.event.MethodEntryEvent
import com.sun.jdi.event.MethodExitEvent
import com.sun.jdi.event.ModificationWatchpointEvent
import com.sun.jdi.event.MonitorContendedEnterEvent
import com.sun.jdi.event.MonitorContendedEnteredEvent
import com.sun.jdi.event.MonitorWaitEvent
import com.sun.jdi.event.MonitorWaitedEvent
import com.sun.jdi.event.StepEvent
import com.sun.jdi.event.ThreadDeathEvent
import com.sun.jdi.event.ThreadStartEvent
import com.sun.jdi.event.VMDeathEvent
import com.sun.jdi.event.VMDisconnectEvent
import com.sun.jdi.event.VMStartEvent
import com.sun.jdi.event.WatchpointEvent
import com.sun.jdi.request.StepRequest
import mu.KotlinLogging


val LOG = KotlinLogging.logger {}

const val className = "ga.rugal.DebuggeeMain"

@Throws(IncompatibleThreadStateException::class, AbsentInformationException::class)
fun displayVariables(event: LocatableEvent) {
  val stackFrame: StackFrame = event.thread().frame(0)
  if (stackFrame.location().toString().contains(className)) {
    LOG.info { "Variables at ${stackFrame.location()} > " }

    for ((key, value) in stackFrame.getValues(stackFrame.visibleVariables())) {
      LOG.info { "${key.name()} = $value" }
    }
  }
}

private fun eventLoop(vm: VirtualMachine) {
  try {
    var set = vm.eventQueue().remove()
    while (set != null) {
      for (e in set) {
        when (e) {
          // vm
          is VMStartEvent -> {
            // TODO: wait for input
            LOG.info { "Started" }
            // TODO: emulate class run
            vm.eventRequestManager().createClassPrepareRequest().also {
              it.addClassFilter(className)
              it.enable()
            }
            vm.resume()
          }

          is VMDeathEvent -> {
            LOG.info { "VM is about to terminate abnormally" }
          }

          is VMDisconnectEvent -> {
            LOG.info { "VM is about to disconnect" }
          }

          // class
          is ClassPrepareEvent -> {
            LOG.info { "Class prepare" }
            // add breakpoint
            vm.eventRequestManager()
              .createBreakpointRequest(vm.classesByName(className)[0].locationsOfLine(6)[0])
              .also { it.enable() }

            vm.resume()
          }

          is ClassUnloadEvent -> {}

          // thread
          is ThreadStartEvent -> {}
          is ThreadDeathEvent -> {}

          // locatable
          is LocatableEvent -> when (e) {
            // watch point
            is WatchpointEvent -> when (e) {
              is AccessWatchpointEvent -> {}
              is ModificationWatchpointEvent -> {}
            }

            is BreakpointEvent -> {
              LOG.info { "break point reached" }
              // TODO: wait for input
              // display stack frame variable
              displayVariables(e)
              // move next
              vm.eventRequestManager()
                .createStepRequest(e.thread(), StepRequest.STEP_LINE, StepRequest.STEP_OVER)
                .also {
                  it.addCountFilter(1)
                  it.enable()
                }

              vm.resume()
            }

            is ExceptionEvent -> {}
            is MethodEntryEvent -> {}
            is MethodExitEvent -> {}
            is MonitorContendedEnteredEvent -> {}
            is MonitorContendedEnterEvent -> {}
            is MonitorWaitedEvent -> {}
            is MonitorWaitEvent -> {}
            is StepEvent -> {
              // TODO: wait for input
              LOG.info { "step" }
              displayVariables(e)
              vm.resume()
            }
          }

          else -> vm.resume()
        }
      }
      set = vm.eventQueue().remove()
    }
  } catch (_: VMDisconnectedException) {
    LOG.info { "Disconnected with debuggee" }
  }
}

private fun getVirtualMachine(): VirtualMachine? {
  val connector =
    Bootstrap.virtualMachineManager().attachingConnectors().firstOrNull { it.name() == "com.sun.jdi.SocketAttach" }
      ?: return null

  val argument = connector.defaultArguments().also { it["port"]!!.setValue("8000") }
  return connector.attach(argument)
}

//  eventLoop(getVirtualMachine() ?: return)

fun main(args: Array<String>) {

  /*
        In this forth tutorial we will finally look at creating a multi-window text GUI, all based on text. Just like
        the Screen-layer in the previous tutorial was based on the lower-level Terminal layer, the GUI classes we will
        use here are all build upon the Screen interface. Because of this, if you use these classes, you should never
        interact with the underlying Screen that backs the GUI directly, as it might modify the screen in a way the
        GUI isn't aware of.

        The GUI system is designed around a background surface that is usually static, but can have components, and
        multiple windows. The recommended approach it to make all windows modal and not let the user switch between
        windows, but the latter can also be done. Components are added to windows by using a layout manager that
        determines the position of each component.
         */
  val terminalFactory = DefaultTerminalFactory()
  var screen: Screen? = null

  try {
    /*
            The DefaultTerminalFactory class doesn't provide any helper method for creating a Text GUI, you'll need to
             get a Screen like we did in the previous tutorial and start it so it puts the terminal in private mode.
             */
    screen = terminalFactory.createScreen()
    screen.startScreen()

    /*
            There are a couple of different constructors to MultiWindowTextGUI, we are going to go with the defaults for
            most of these values. The one thing to consider is threading; with the default options, lanterna will use
            the calling thread for all UI operations which mean that you are basically letting the calling thread block
            until the GUI is shut down. There is a separate TextGUIThread implementaiton you can use if you'd like
            Lanterna to create a dedicated UI thread and not lock the caller. Just like with AWT and Swing, you should
            be scheduling any kind of UI operation to always run on the UI thread but lanterna tries to be best-effort
            if you attempt to mutate the GUI from another thread. Another default setting that will be applied is that
            the background of the GUI will be solid blue.
             */
    val textGUI: WindowBasedTextGUI = MultiWindowTextGUI(screen)

    /*
            Creating a new window is relatively uncomplicated, you can optionally supply a title for the window
             */
    val window: Window = BasicWindow("My Root Window")

    /*
            The window has no content initially, you need to call setComponent to populate it with something. In this
            case, and quite often in fact, you'll want to use more than one component so we'll create a composite
            'Panel' component that can hold multiple sub-components. This is where we decide what the layout manager
            should be.
             */
    val contentPanel = Panel()

    /*
             * Lanterna contains a number of built-in layout managers, the simplest one being LinearLayout that simply
             * arranges components in either a horizontal or a vertical line. In this tutorial, we'll use the GridLayout
             * which is based on the layout manager with the same name in SWT. In the constructor above we have
             * specified that we want to have a grid with two columns, below we customize the layout further by adding
             * some spacing between the columns.
             */
    val layout = GridLayout(2)
    contentPanel.layoutManager = layout

    /*
            One of the most basic components is the Label, which simply displays a static text. In the example below,
            we use the layout data field attached to each component to give the layout manager extra hints about how it
            should be placed. Obviously the layout data has to be created from the same layout manager as the container
            is using, otherwise it will be ignored.
             */
    val title = Label("This is a label that spans two columns")
    title.setLayoutData(
      GridLayout.createLayoutData(
        GridLayout.Alignment.BEGINNING,  // Horizontal alignment in the grid cell if the cell is larger than the component's preferred size
        GridLayout.Alignment.BEGINNING,  // Vertical alignment in the grid cell if the cell is larger than the component's preferred size
        true,  // Give the component extra horizontal space if available
        false,  // Give the component extra vertical space if available
        2,  // Horizontal span
        1
      )
    ) // Vertical span
    contentPanel.addComponent(title)

    /*
            Since the grid has two columns, we can do something like this to add components when we don't need to
            customize them any further.
             */contentPanel.addComponent(Label("Text Box (aligned)"))
    contentPanel.addComponent(
      TextBox()
        .setLayoutData(GridLayout.createLayoutData(GridLayout.Alignment.BEGINNING, GridLayout.Alignment.CENTER))
    )

    /*
            Here is an example of customizing the regular text box component so it masks the content and can work for
            password input.
             */contentPanel.addComponent(Label("Password Box (right aligned)"))
    contentPanel.addComponent(
      TextBox()
        .setMask('*')
        .setLayoutData(GridLayout.createLayoutData(GridLayout.Alignment.END, GridLayout.Alignment.CENTER))
    )

    /*
            While we are not going to demonstrate all components here, here is an example of combo-boxes, one that is
            read-only and one that is editable.
             */contentPanel.addComponent(Label("Read-only Combo Box (forced size)"))
    val timezonesAsStrings: List<String> = ArrayList(listOf(*TimeZone.getAvailableIDs()))
    val readOnlyComboBox = ComboBox(timezonesAsStrings)
    readOnlyComboBox.setReadOnly(true)
    readOnlyComboBox.setPreferredSize(TerminalSize(20, 1))
    contentPanel.addComponent(readOnlyComboBox)
    contentPanel.addComponent(Label("Editable Combo Box (filled)"))
    contentPanel.addComponent(
      ComboBox("Item #1", "Item #2", "Item #3", "Item #4")
        .setReadOnly(false)
        .setLayoutData(GridLayout.createHorizontallyFilledLayoutData(1))
    )

    /*
            Some user interactions, like buttons, work by registering callback methods. In this example here, we're
            using one of the pre-defined dialogs when the button is triggered.
             */contentPanel.addComponent(Label("Button (centered)"))
    contentPanel.addComponent(Button("Button") {
      MessageDialog.showMessageDialog(
        textGUI,
        "MessageBox",
        "This is a message box",
        MessageDialogButton.OK
      )
    }.setLayoutData(GridLayout.createLayoutData(GridLayout.Alignment.CENTER, GridLayout.Alignment.CENTER)))

    /*
            Close off with an empty row and a separator, then a button to close the window
             */contentPanel.addComponent(
      EmptySpace()
        .setLayoutData(
          GridLayout.createHorizontallyFilledLayoutData(2)
        )
    )
    contentPanel.addComponent(
      Separator(Direction.HORIZONTAL)
        .setLayoutData(
          GridLayout.createHorizontallyFilledLayoutData(2)
        )
    )
    contentPanel.addComponent(
      Button("Close", window::close).setLayoutData(
        GridLayout.createHorizontallyEndAlignedLayoutData(2)
      )
    )

    /*
            We now have the content panel fully populated with components. A common mistake is to forget to attach it to
            the window, so let's make sure to do that.
             */
    window.component = contentPanel

    /*
            Now the window is created and fully populated. As discussed above regarding the threading model, we have the
            option to fire off the GUI here and then later on decide when we want to stop it. In order for this to work,
            you need a dedicated UI thread to run all the GUI operations, usually done by passing in a
            SeparateTextGUIThread object when you create the TextGUI. In this tutorial, we are using the conceptually
            simpler SameTextGUIThread, which essentially hijacks the caller thread and uses it as the GUI thread until
            some stop condition is met. The absolutely simplest way to do this is to simply ask lanterna to display the
            window and wait for it to be closed. This will initiate the event loop and make the GUI functional. In the
            "Close" button above, we tied a call to the close() method on the Window object when the button is
            triggered, this will then break the even loop and our call finally returns.
             */
    textGUI.addWindowAndWait(window)

    /*
            When our call has returned, the window is closed and no longer visible. The screen still contains the last
            state the TextGUI left it in, so we can easily add and display another window without any flickering. In
            this case, we want to shut down the whole thing and return to the ordinary prompt. We just need to stop the
            underlying Screen for this, the TextGUI system does not require any additional disassembly.
             */
  } catch (e: IOException) {
    e.printStackTrace()
  } finally {
    if (screen != null) {
      try {
        screen.stopScreen()
      } catch (e: IOException) {
        e.printStackTrace()
      }
    }
  }
}
