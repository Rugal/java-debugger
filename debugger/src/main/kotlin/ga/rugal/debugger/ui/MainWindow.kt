package ga.rugal.debugger.ui

import java.util.concurrent.atomic.AtomicBoolean
import com.googlecode.lanterna.TerminalPosition
import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.gui2.BasicWindow
import com.googlecode.lanterna.gui2.Borders
import com.googlecode.lanterna.gui2.Direction
import com.googlecode.lanterna.gui2.LinearLayout
import com.googlecode.lanterna.gui2.Panel
import com.googlecode.lanterna.gui2.Window
import com.googlecode.lanterna.gui2.WindowListener
import com.googlecode.lanterna.input.KeyStroke

object MainWindow : BasicWindow() {
  private val mainPanel = Panel()
  private val leftPanel = Panel()
  private val rightPanel = Panel()
  private val stackTraceOutPanel = Panel()
  val stackTracePanel = Panel()

  private val sourceOutPanel = Panel()
  val sourcePanel = Panel()

  private val stackFrameOutPanel = Panel()
  val stackFramePanel = Panel()

  init {
    this.component = mainPanel
    this.setHints(
      listOf(
        Window.Hint.EXPANDED,
        Window.Hint.CENTERED,
        Window.Hint.NO_POST_RENDERING,
        Window.Hint.NO_DECORATIONS
      )
    )
    this.addWindowListener(object : WindowListener {
      override fun onInput(basePane: Window?, keyStroke: KeyStroke, deliverEvent: AtomicBoolean?) {
        // switch between panel
        when (keyStroke.character) {
          't' -> true
          'f' -> true
          's' -> true
          else -> false
        }

      }

      override fun onUnhandledInput(basePane: Window?, keyStroke: KeyStroke?, hasBeenHandled: AtomicBoolean?) {
      }

      override fun onResized(window: Window?, oldSize: TerminalSize?, newSize: TerminalSize) {
        leftPanel.preferredSize = TerminalSize((newSize.columns * 0.25).toInt(), newSize.rows)
        rightPanel.preferredSize = TerminalSize((newSize.columns * 0.75).toInt(), newSize.rows)

        stackTraceOutPanel.preferredSize = TerminalSize(leftPanel.preferredSize.columns, leftPanel.preferredSize.rows)
        stackTracePanel.preferredSize = TerminalSize(leftPanel.preferredSize.columns, leftPanel.preferredSize.rows)

        sourceOutPanel.preferredSize = TerminalSize(
          rightPanel.preferredSize.columns,
          (rightPanel.preferredSize.rows * 0.6).toInt()
        )
        sourcePanel.preferredSize = TerminalSize(
          rightPanel.preferredSize.columns,
          (rightPanel.preferredSize.rows * 0.6).toInt()
        )

        stackFrameOutPanel.preferredSize = TerminalSize(
          rightPanel.preferredSize.columns,
          (rightPanel.preferredSize.rows * 0.5).toInt()
        )
        stackFramePanel.preferredSize = TerminalSize(
          rightPanel.preferredSize.columns,
          (rightPanel.preferredSize.rows * 0.5).toInt()
        )
      }

      override fun onMoved(window: Window?, oldPosition: TerminalPosition?, newPosition: TerminalPosition?) {
      }
    })

    this.mainPanel.also {
      it.layoutManager = LinearLayout(Direction.HORIZONTAL)
      it.addComponent(leftPanel)
      it.addComponent(rightPanel)
    }

    this.leftPanel.also {
      it.layoutManager = LinearLayout(Direction.VERTICAL)
      it.addComponent(this.stackTraceOutPanel)
    }

    this.stackTraceOutPanel.also {
      it.addComponent(this.stackTracePanel.withBorder(Borders.singleLine("Stack Trace")))
    }

    this.rightPanel.also {
      it.layoutManager = LinearLayout(Direction.VERTICAL)
      it.addComponent(this.sourceOutPanel)
      it.addComponent(this.stackFrameOutPanel)
    }

    this.sourceOutPanel.also {
      it.addComponent(this.sourcePanel.withBorder(Borders.singleLine("Source")))
    }

    this.stackFrameOutPanel.also {
      it.addComponent(this.stackFramePanel.withBorder(Borders.singleLine("Stack Frame")))
    }
  }

  private fun highlightPanel(toHighlight: Panel, other: List<Panel>) {
//    toHighlight.withBorder()
  }
}
