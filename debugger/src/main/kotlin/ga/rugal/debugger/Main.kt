package ga.rugal.debugger

import com.sun.jdi.AbsentInformationException
import com.sun.jdi.Bootstrap
import com.sun.jdi.IncompatibleThreadStateException
import com.sun.jdi.StackFrame
import com.sun.jdi.VMDisconnectedException
import com.sun.jdi.VirtualMachine
import com.sun.jdi.connect.AttachingConnector
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

fun main(args: Array<String>) {
  val connector: AttachingConnector =
    Bootstrap.virtualMachineManager().attachingConnectors().firstOrNull { it.name() == "com.sun.jdi.SocketAttach" }
      ?: return

  val argument = connector.defaultArguments().also { it["port"]!!.setValue("8000") }
  val vm: VirtualMachine = connector.attach(argument)
  eventLoop(vm)
}
