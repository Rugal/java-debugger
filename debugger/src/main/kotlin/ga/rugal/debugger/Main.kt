package ga.rugal.debugger

import com.sun.jdi.Bootstrap
import com.sun.jdi.ClassType
import com.sun.jdi.VMDisconnectedException
import com.sun.jdi.VirtualMachine
import com.sun.jdi.connect.AttachingConnector
import com.sun.jdi.event.BreakpointEvent
import com.sun.jdi.event.ClassPrepareEvent
import com.sun.jdi.event.VMDeathEvent
import com.sun.jdi.event.VMDisconnectEvent
import com.sun.jdi.event.VMStartEvent
import mu.KotlinLogging

val LOG = KotlinLogging.logger {}

fun setBreakPoints(vm: VirtualMachine, event: ClassPrepareEvent) {
  val classType: ClassType = event.referenceType() as ClassType
  arrayOf(6, 7).forEach { lineNumber ->
    vm.eventRequestManager()
      .createBreakpointRequest(classType.locationsOfLine(lineNumber)[0])
      .also { it.enable() }
  }
}

fun main(args: Array<String>) {
  val connector: AttachingConnector =
    Bootstrap.virtualMachineManager().attachingConnectors().firstOrNull { it.name() == "com.sun.jdi.SocketAttach" }
      ?: return

  val argument = connector.defaultArguments().also { it["port"]!!.setValue("8000") }

  val vm = connector.attach(argument)

  try {
    var set = vm.eventQueue().remove()
    while (set != null) {
      set.forEach { e ->
        when (e) {
          is VMStartEvent -> {
            LOG.info { "Started" }
            vm.eventRequestManager().createClassPrepareRequest().also {
              it.addClassFilter("ga.rugal.DebuggeeMain")
              it.enable()
            }
            vm.resume();
          }

          is ClassPrepareEvent -> {

            LOG.info { "Class prepare" }
            setBreakPoints(vm, e)
            vm.resume();
          }

          is BreakpointEvent -> LOG.info { "breakpoint" }
          is VMDeathEvent -> {
            LOG.info { "Death" }
            vm.resume();
          }

          is VMDisconnectEvent -> {
            LOG.info { "Disconnected" }
            vm.resume()
          }

          else -> {
            LOG.info { e.javaClass }
            vm.resume()
          }
        }
      }
      set = vm.eventQueue().remove()
    }
  } catch (_: VMDisconnectedException) {
    LOG.info { "Disconnected with debuggee" }
  }
}
