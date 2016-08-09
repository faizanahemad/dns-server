package io.faizan

import java.awt._
import java.awt.event._
import java.awt.geom.Rectangle2D


class SplashDisplay(title: String) extends Frame(title) with ActionListener {
  private var splash = Option.empty[SplashScreen]
  private var g = Option.empty[Graphics2D]
  private var progressArea = Option.empty[Rectangle2D.Double]
  private val dim = (400,300)


  private def renderSplashFrame(display: String) {
    val textX = ((dim._1 - 8.974d*(display.length+3))/2).toInt
    g.foreach((gr) => {
      gr.setComposite(AlphaComposite.Clear)
      gr.fillRect(0, 180, dim._1, 80)
      gr.setPaintMode()
      gr.setColor(Color.BLACK)
      gr.drawString(display + "...", textX, 230)
    })
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

  private def splashProgress(pct: Int) {
    splash.foreach(mySplash=>{
      g.foreach(splashGraphics=>{
        val splashProgressArea = progressArea.get
        splashGraphics.setPaint(Color.WHITE)
        splashGraphics.fill(splashProgressArea)
        // draw an outline
        splashGraphics.setPaint(Color.YELLOW)
        splashGraphics.draw(splashProgressArea)
        // Calculate the width corresponding to the correct percentage
        val x = splashProgressArea.getMinX.toInt
        val y = splashProgressArea.getMinY.toInt
        val wid = splashProgressArea.getWidth.toInt
        val hgt = splashProgressArea.getHeight.toInt
        val doneWidth = (Math.min(pct, 100) * wid / 100.0f).round
        // fill the done part one pixel smaller than the outline
        splashGraphics.setPaint(Color.BLUE)
        splashGraphics.fillRect(x, y + 1, doneWidth, hgt - 1)
        // make sure it's displayed
        mySplash.update()
      })
    })
  }

  private def init = {
    splash = Option(SplashScreen.getSplashScreen)
    splash.foreach((s) => {
      g = Option(s.createGraphics).map(g=>{
        val font = new Font("Dialog", Font.ROMAN_BASELINE, 18)
        g.setFont(font)
        g
      })
    })
    progressArea = splash.map(mySplash=>{
      val ssDim = mySplash.getSize
      val height = ssDim.height
      val width = ssDim.width
      val splashProgressArea = new Rectangle2D.Double(width * .05, height * .88, width*0.90, 3)
      splashProgressArea
    })
    setSize(dim._1, dim._2)
    setLayout(new BorderLayout)
    createMenuBar
  }

  private def renderSplash(text: String, pct:Int): Unit = {
    splashProgress(pct)
    renderSplashFrame(text)
    splash.get.update()
  }

  private def stopSplash() = {
    splash.get.close()
  }

  private def renderText(text: String, pct:Int): Unit = {
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


