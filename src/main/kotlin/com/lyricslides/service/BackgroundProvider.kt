package com.lyricslides.service

import com.lyricslides.model.SlideBackground
import java.awt.*
import java.awt.geom.AffineTransform
import java.awt.geom.Ellipse2D
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Provides built-in background images, generated procedurally so the app
 * works without bundled asset files. Caches generated images in memory.
 */
class BackgroundProvider {

    companion object {
        const val WIDTH = SlideGenerator.SLIDE_WIDTH
        const val HEIGHT = SlideGenerator.SLIDE_HEIGHT

        // Identifier prefix for built-in (procedurally generated) backgrounds.
        const val BUILTIN_PREFIX = "builtin:"
    }

    private val cache = mutableMapOf<String, BufferedImage>()

    /**
     * The full list of built-in backgrounds, matched by id.
     * The id is stored in SlideBackground.path with the prefix "builtin:".
     */
    fun getBuiltInBackgrounds(): List<SlideBackground> = listOf(
        SlideBackground("Dark Gradient", "${BUILTIN_PREFIX}dark_gradient", true, "Dark"),
        SlideBackground("Blue Sky", "${BUILTIN_PREFIX}blue_sky", true, "Nature"),
        SlideBackground("Sunset", "${BUILTIN_PREFIX}sunset", true, "Nature"),
        SlideBackground("Forest", "${BUILTIN_PREFIX}forest", true, "Nature"),
        SlideBackground("Ocean", "${BUILTIN_PREFIX}ocean", true, "Nature"),
        SlideBackground("Stained Glass", "${BUILTIN_PREFIX}stained_glass", true, "Church"),
        SlideBackground("Wooden Cross", "${BUILTIN_PREFIX}wooden_cross", true, "Church"),
        SlideBackground("Heavenly Light", "${BUILTIN_PREFIX}heavenly_light", true, "Church"),
        SlideBackground("Abstract Purple", "${BUILTIN_PREFIX}abstract_purple", true, "Abstract"),
        SlideBackground("Abstract Blue", "${BUILTIN_PREFIX}abstract_blue", true, "Abstract"),
        SlideBackground("Stars", "${BUILTIN_PREFIX}stars", true, "Dark"),
        SlideBackground("Plain Black", "${BUILTIN_PREFIX}plain_black", true, "Simple"),
        SlideBackground("Plain White", "${BUILTIN_PREFIX}plain_white", true, "Simple"),
        SlideBackground("Soft Gray", "${BUILTIN_PREFIX}soft_gray", true, "Simple"),
    )

    /**
     * Load a background as a BufferedImage.
     * Handles both built-in (generated) and custom file-path backgrounds.
     */
    fun loadBackground(background: SlideBackground?): BufferedImage? {
        if (background == null) return null
        val path = background.path

        return when {
            path.startsWith(BUILTIN_PREFIX) -> {
                val id = path.removePrefix(BUILTIN_PREFIX)
                cache.getOrPut(id) { generateBuiltIn(id) }
            }
            else -> {
                // Custom file path
                try {
                    val file = File(path)
                    if (file.exists() && file.isFile) ImageIO.read(file) else null
                } catch (_: Exception) {
                    null
                }
            }
        }
    }

    /**
     * Generate a small thumbnail version for preview in the picker.
     * Returns a 320x180 (16:9) thumbnail.
     */
    fun loadThumbnail(background: SlideBackground, width: Int = 320, height: Int = 180): BufferedImage? {
        val full = loadBackground(background) ?: return null
        val thumb = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g2d = thumb.createGraphics()
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g2d.drawImage(full.getScaledInstance(width, height, Image.SCALE_SMOOTH), 0, 0, null)
        g2d.dispose()
        return thumb
    }

    /**
     * Generate a procedural background image by id.
     */
    private fun generateBuiltIn(id: String): BufferedImage {
        val img = BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        when (id) {
            "dark_gradient" -> drawDarkGradient(g)
            "blue_sky" -> drawBlueSky(g)
            "sunset" -> drawSunset(g)
            "forest" -> drawForest(g)
            "ocean" -> drawOcean(g)
            "stained_glass" -> drawStainedGlass(g)
            "wooden_cross" -> drawWoodenCross(g)
            "heavenly_light" -> drawHeavenlyLight(g)
            "abstract_purple" -> drawAbstractPurple(g)
            "abstract_blue" -> drawAbstractBlue(g)
            "stars" -> drawStars(g)
            "plain_black" -> drawPlain(g, Color.BLACK)
            "plain_white" -> drawPlain(g, Color.WHITE)
            "soft_gray" -> drawPlain(g, Color(60, 60, 70))
            else -> drawDarkGradient(g)
        }

        g.dispose()
        return img
    }

    // ────────────────────────────────────────────────────────────────
    // Procedural drawing methods
    // ────────────────────────────────────────────────────────────────

    private fun drawPlain(g: Graphics2D, color: Color) {
        g.color = color
        g.fillRect(0, 0, WIDTH, HEIGHT)
    }

    private fun drawDarkGradient(g: Graphics2D) {
        g.paint = GradientPaint(
            0f, 0f, Color(15, 15, 40),
            0f, HEIGHT.toFloat(), Color(40, 40, 90)
        )
        g.fillRect(0, 0, WIDTH, HEIGHT)
    }

    private fun drawBlueSky(g: Graphics2D) {
        g.paint = GradientPaint(
            0f, 0f, Color(110, 180, 240),
            0f, HEIGHT.toFloat(), Color(200, 230, 255)
        )
        g.fillRect(0, 0, WIDTH, HEIGHT)

        // Soft clouds
        val rng = Random(42)
        g.color = Color(255, 255, 255, 90)
        repeat(6) {
            val cx = rng.nextInt(WIDTH)
            val cy = rng.nextInt(HEIGHT / 2)
            val cw = 400 + rng.nextInt(400)
            val ch = 120 + rng.nextInt(80)
            g.fill(Ellipse2D.Float(cx.toFloat(), cy.toFloat(), cw.toFloat(), ch.toFloat()))
        }
    }

    private fun drawSunset(g: Graphics2D) {
        // Multi-stop gradient from deep purple to orange
        val paint = LinearGradientPaint(
            0f, 0f, 0f, HEIGHT.toFloat(),
            floatArrayOf(0f, 0.4f, 0.7f, 1f),
            arrayOf(
                Color(40, 20, 60),
                Color(180, 60, 100),
                Color(240, 140, 80),
                Color(255, 210, 140)
            )
        )
        g.paint = paint
        g.fillRect(0, 0, WIDTH, HEIGHT)

        // Sun
        val radialSun = RadialGradientPaint(
            (WIDTH * 0.5f), (HEIGHT * 0.75f), 300f,
            floatArrayOf(0f, 1f),
            arrayOf(Color(255, 240, 200, 200), Color(255, 180, 100, 0))
        )
        g.paint = radialSun
        g.fill(Ellipse2D.Float(WIDTH * 0.5f - 300, HEIGHT * 0.75f - 300, 600f, 600f))
    }

    private fun drawForest(g: Graphics2D) {
        g.paint = GradientPaint(
            0f, 0f, Color(20, 60, 35),
            0f, HEIGHT.toFloat(), Color(45, 100, 55)
        )
        g.fillRect(0, 0, WIDTH, HEIGHT)

        // Vertical "tree trunk" strokes
        val rng = Random(123)
        g.color = Color(15, 35, 20, 180)
        repeat(14) {
            val x = rng.nextInt(WIDTH)
            val w = 40 + rng.nextInt(100)
            g.fill(Rectangle2D.Float(x.toFloat(), 0f, w.toFloat(), HEIGHT.toFloat()))
        }
    }

    private fun drawOcean(g: Graphics2D) {
        g.paint = GradientPaint(
            0f, 0f, Color(20, 60, 110),
            0f, HEIGHT.toFloat(), Color(10, 30, 60)
        )
        g.fillRect(0, 0, WIDTH, HEIGHT)

        // Wave lines
        val rng = Random(7)
        g.color = Color(180, 220, 255, 40)
        g.stroke = BasicStroke(3f)
        for (i in 0 until 40) {
            val y = rng.nextInt(HEIGHT)
            val amplitude = 10 + rng.nextInt(25)
            val prevPath = java.awt.geom.Path2D.Float()
            prevPath.moveTo(0.0, y.toDouble())
            var x = 0
            while (x < WIDTH) {
                prevPath.lineTo(x.toDouble(), (y + amplitude * sin(x * 0.01)).toDouble())
                x += 20
            }
            g.draw(prevPath)
        }
    }

    private fun drawStainedGlass(g: Graphics2D) {
        drawPlain(g, Color(10, 10, 30))
        val rng = Random(99)
        val colors = listOf(
            Color(180, 40, 60), Color(40, 80, 180), Color(230, 180, 50),
            Color(60, 140, 100), Color(130, 60, 170), Color(200, 100, 60)
        )

        // Random polygons filling the screen
        val cellW = 240
        val cellH = 180
        for (yi in 0 until (HEIGHT / cellH + 1)) {
            for (xi in 0 until (WIDTH / cellW + 1)) {
                val cx = xi * cellW
                val cy = yi * cellH
                val color = colors[rng.nextInt(colors.size)]
                g.color = Color(color.red, color.green, color.blue, 180)
                val px = intArrayOf(cx, cx + cellW, cx + cellW, cx)
                val py = intArrayOf(cy, cy, cy + cellH, cy + cellH)
                g.fillPolygon(px, py, 4)
                // Dark leading between cells
                g.color = Color(5, 5, 15, 220)
                g.stroke = BasicStroke(6f)
                g.drawPolygon(px, py, 4)
            }
        }

        // Radial light overlay
        val radial = RadialGradientPaint(
            (WIDTH / 2f), (HEIGHT / 2f), (WIDTH / 1.2f),
            floatArrayOf(0f, 1f),
            arrayOf(Color(255, 255, 230, 80), Color(0, 0, 0, 120))
        )
        g.paint = radial
        g.fillRect(0, 0, WIDTH, HEIGHT)
    }

    private fun drawWoodenCross(g: Graphics2D) {
        // Warm wooden background
        g.paint = GradientPaint(
            0f, 0f, Color(50, 25, 15),
            0f, HEIGHT.toFloat(), Color(90, 50, 25)
        )
        g.fillRect(0, 0, WIDTH, HEIGHT)

        // Wood grain lines
        val rng = Random(5)
        g.color = Color(30, 15, 8, 120)
        for (i in 0 until 25) {
            val y = rng.nextInt(HEIGHT)
            g.stroke = BasicStroke(1f + rng.nextFloat() * 3f)
            g.drawLine(0, y, WIDTH, y + rng.nextInt(40) - 20)
        }

        // Cross silhouette (soft, off-center)
        val crossColor = Color(0, 0, 0, 110)
        g.color = crossColor
        val cx = WIDTH * 3 / 4
        val cy = HEIGHT / 2
        val vertW = 60
        val horzW = 240
        val vertH = 500
        val horzH = 50
        g.fillRect(cx - vertW / 2, cy - vertH / 2, vertW, vertH)
        g.fillRect(cx - horzW / 2, cy - horzH / 2 - 80, horzW, horzH)

        // Subtle radial light
        val radial = RadialGradientPaint(
            (WIDTH / 4f), (HEIGHT / 3f), 900f,
            floatArrayOf(0f, 1f),
            arrayOf(Color(255, 220, 150, 80), Color(0, 0, 0, 0))
        )
        g.paint = radial
        g.fillRect(0, 0, WIDTH, HEIGHT)
    }

    private fun drawHeavenlyLight(g: Graphics2D) {
        drawPlain(g, Color(18, 20, 40))

        // Light rays from top center
        val cx = WIDTH / 2
        val cy = HEIGHT / 4
        val rng = Random(11)
        for (i in 0 until 24) {
            val angle = Math.PI / 6 + (i / 24.0) * (2 * Math.PI / 3)
            val len = 1800
            val x2 = (cx + len * cos(angle)).toInt()
            val y2 = (cy + len * sin(angle)).toInt()
            g.stroke = BasicStroke(40f + rng.nextFloat() * 30f)
            g.color = Color(255, 240, 180, 20 + rng.nextInt(20))
            g.drawLine(cx, cy, x2, y2)
        }

        // Bright center
        val radial = RadialGradientPaint(
            cx.toFloat(), cy.toFloat(), 600f,
            floatArrayOf(0f, 1f),
            arrayOf(Color(255, 250, 220, 200), Color(255, 250, 220, 0))
        )
        g.paint = radial
        g.fill(Ellipse2D.Float(cx - 600f, cy - 600f, 1200f, 1200f))
    }

    private fun drawAbstractPurple(g: Graphics2D) {
        g.paint = GradientPaint(
            0f, 0f, Color(60, 20, 90),
            WIDTH.toFloat(), HEIGHT.toFloat(), Color(140, 50, 170)
        )
        g.fillRect(0, 0, WIDTH, HEIGHT)

        val rng = Random(33)
        repeat(8) {
            val r = 200 + rng.nextInt(300)
            val cx = rng.nextInt(WIDTH)
            val cy = rng.nextInt(HEIGHT)
            val radial = RadialGradientPaint(
                cx.toFloat(), cy.toFloat(), r.toFloat(),
                floatArrayOf(0f, 1f),
                arrayOf(Color(220, 160, 255, 80), Color(220, 160, 255, 0))
            )
            g.paint = radial
            g.fill(Ellipse2D.Float((cx - r).toFloat(), (cy - r).toFloat(), (r * 2).toFloat(), (r * 2).toFloat()))
        }
    }

    private fun drawAbstractBlue(g: Graphics2D) {
        g.paint = GradientPaint(
            0f, 0f, Color(20, 40, 90),
            WIDTH.toFloat(), HEIGHT.toFloat(), Color(50, 120, 200)
        )
        g.fillRect(0, 0, WIDTH, HEIGHT)

        val rng = Random(77)
        repeat(10) {
            val r = 200 + rng.nextInt(350)
            val cx = rng.nextInt(WIDTH)
            val cy = rng.nextInt(HEIGHT)
            val radial = RadialGradientPaint(
                cx.toFloat(), cy.toFloat(), r.toFloat(),
                floatArrayOf(0f, 1f),
                arrayOf(Color(180, 220, 255, 80), Color(180, 220, 255, 0))
            )
            g.paint = radial
            g.fill(Ellipse2D.Float((cx - r).toFloat(), (cy - r).toFloat(), (r * 2).toFloat(), (r * 2).toFloat()))
        }
    }

    private fun drawStars(g: Graphics2D) {
        g.paint = GradientPaint(
            0f, 0f, Color(5, 5, 20),
            0f, HEIGHT.toFloat(), Color(15, 15, 40)
        )
        g.fillRect(0, 0, WIDTH, HEIGHT)

        val rng = Random(123)
        // Large glowing stars
        repeat(25) {
            val x = rng.nextInt(WIDTH)
            val y = rng.nextInt(HEIGHT)
            val size = 3 + rng.nextInt(6)
            val glow = size * 4
            val radial = RadialGradientPaint(
                x.toFloat(), y.toFloat(), glow.toFloat(),
                floatArrayOf(0f, 1f),
                arrayOf(Color(255, 255, 220, 220), Color(255, 255, 220, 0))
            )
            g.paint = radial
            g.fill(Ellipse2D.Float((x - glow).toFloat(), (y - glow).toFloat(), (glow * 2).toFloat(), (glow * 2).toFloat()))
            g.color = Color.WHITE
            g.fillOval(x - size / 2, y - size / 2, size, size)
        }
        // Smaller scattered stars
        repeat(250) {
            val x = rng.nextInt(WIDTH)
            val y = rng.nextInt(HEIGHT)
            val alpha = 100 + rng.nextInt(155)
            g.color = Color(255, 255, 240, alpha)
            g.fillOval(x, y, 2, 2)
        }
    }
}
