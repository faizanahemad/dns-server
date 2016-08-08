package io.faizan

import java.awt._
import java.awt.event._


class SplashDisplay(title: String) extends Frame(title) with ActionListener {
  private var dotCounter = 0
  private var splash = Option.empty[SplashScreen]
  private var g = Option.empty[Graphics2D]


  private def renderSplashFrame(display: String) {
    g.foreach((gr) => {
      gr.setComposite(AlphaComposite.Clear)
      gr.fillRect(140, 180, 220, 80)
      gr.setPaintMode()
      gr.setColor(Color.BLACK)
      gr.drawString(display + getDots, 140, 230)
    })
  }

  private def getDots: String = {
    dotCounter += 1
    (1 to dotCounter % 5).foldLeft[String](" .") { (z, i) => z + "." }
  }

  private def createMenuBar = {
    val m1: Menu = new Menu("File")
    val mi1: MenuItem = new MenuItem("Exit")
    m1.add(mi1)
    mi1.addActionListener(this)
    this.addWindowListener(closeWindow)
    val mb: MenuBar = new MenuBar
    setMenuBar(mb)
    mb.add(m1)
  }

  private def init = {
    splash = Option(SplashScreen.getSplashScreen)
    splash.foreach((s) => g = Option(s.createGraphics))
    setSize(300, 200)
    setLayout(new BorderLayout)
    createMenuBar
  }

  private def renderSplash(text: String): Unit = {
    renderSplashFrame(text)
    splash.get.update()
  }

  private def stopSplash() = {
    splash.get.close()
  }

  private def renderText(text: String): Unit = {
    println(text)
  }

  private def stopText() = {

  }

  def actionPerformed(ae: ActionEvent) {
    System.exit(0)
  }

  private val closeWindow: WindowListener = new WindowAdapter() {
    override def windowClosing(e: WindowEvent) {
      e.getWindow.dispose()
    }
  }
  init
  val render = if (g.isDefined) renderSplash _ else renderText _
  val stop = if (g.isDefined) stopSplash _ else stopText _
}


