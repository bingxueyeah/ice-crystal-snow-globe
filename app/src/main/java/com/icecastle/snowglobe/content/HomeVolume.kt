package com.icecastle.snowglobe.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.ModelComponent
import com.pico.spatial.core.ecs.TransformComponent
import com.pico.spatial.core.ecs.resource.BlendingMode
import com.pico.spatial.core.ecs.resource.MeshResource
import com.pico.spatial.core.ecs.resource.UnlitMaterial
import com.pico.spatial.core.math.Color4
import com.pico.spatial.core.math.EulerAngles
import com.pico.spatial.core.math.Vector3
import com.pico.spatial.ui.design.Button
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.foundation.content.SpatialView
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private const val SGS_MODEL_PATH = "asset://ice_castle_snow_globe.glb"
private const val SGS_MODEL_SCALE = 0.39f

private data class Snowflake(
    val entity: Entity,
    val x: Float,
    val z: Float,
    val phase: Float,
    val speed: Float,
)

private class SnowGlobeScene {
    val root = Entity().apply { setName("LuxuryIceCastleSnowGlobe") }
    val showcase = Entity().apply { setName("SGSGeneratedSnowGlobeShowcase") }
    private val snowRoot = Entity().apply { setName("EnchantedSnowOverlay") }
    private val haloRoot = Entity().apply { setName("CrystalHalo") }
    private val snowflakes = mutableListOf<Snowflake>()

    private val sparkle = UnlitMaterial.create(BlendingMode.ADD).apply {
        setBaseColor(Color4(0.68f, 0.94f, 1f, 0.9f))
        setOpacity(0.9f)
        setDepthWrite(false)
    }
    private val softSnow = UnlitMaterial.create(BlendingMode.TRANSPARENT).apply {
        setBaseColor(Color4(0.96f, 0.99f, 1f, 0.86f))
        setOpacity(0.86f)
        setDepthWrite(false)
    }

    init {
        val generatedModel = Entity.load(SGS_MODEL_PATH).apply {
            setName("SGS_Rodin_High_IceCastleSnowGlobe")
            components[TransformComponent::class.java]?.apply {
                setScaleVector(Vector3(SGS_MODEL_SCALE))
                setPosition(Vector3(0f, 0.015f, 0f))
            }
        }
        showcase.addChild(generatedModel)
        root.addChild(showcase)
        root.addChild(snowRoot)
        root.addChild(haloRoot)
        buildAtmosphericSnow()
        buildCrystalHalo()
    }

    private fun buildAtmosphericSnow() {
        val snowMesh = MeshResource.createSphere(0.0065f)
        val sparkleMesh = MeshResource.createSphere(0.004f)
        val random = Random(20261110)
        repeat(72) { index ->
            val angle = random.nextFloat() * 6.283f
            val radius = 0.12f + random.nextFloat() * 0.25f
            val x = sin(angle) * radius
            val z = cos(angle) * radius
            val y = random.nextFloat() * 0.66f - 0.25f
            val entity = Entity().apply {
                setName("MagicSnow_${index.toString().padStart(2, '0')}")
                components.set(
                    ModelComponent(
                        if (index % 6 == 0) sparkleMesh else snowMesh,
                        if (index % 6 == 0) sparkle else softSnow,
                    )
                )
                components[TransformComponent::class.java]?.setPosition(Vector3(x, y, z))
            }
            snowRoot.addChild(entity)
            snowflakes += Snowflake(
                entity = entity,
                x = x,
                z = z,
                phase = random.nextFloat(),
                speed = 0.45f + random.nextFloat() * 0.9f,
            )
        }
    }

    private fun buildCrystalHalo() {
        val haloMesh = MeshResource.createTorus(0.39f, 0.006f)
        repeat(3) { index ->
            haloRoot.addChild(
                Entity().apply {
                    setName("AuroraHalo_$index")
                    components.set(ModelComponent(haloMesh, sparkle))
                    components[TransformComponent::class.java]?.apply {
                        setPosition(Vector3(0f, -0.20f + index * 0.018f, 0f))
                        setScaleVector(Vector3(1f - index * 0.055f))
                    }
                }
            )
        }
    }

    fun update(time: Float, storm: Boolean) {
        showcase.components[TransformComponent::class.java]?.setEulerAngles(
            EulerAngles(0f, time * if (storm) 8.5f else 3.4f, 0f)
        )
        haloRoot.components[TransformComponent::class.java]?.setEulerAngles(
            EulerAngles(0f, -time * 11f, 0f)
        )
        snowflakes.forEachIndexed { index, flake ->
            val velocity = flake.speed * if (storm) 1.95f else 0.68f
            val fall = (flake.phase + time * velocity) % 1f
            val y = 0.38f - fall * 0.67f
            val swirl = if (storm) 0.075f else 0.022f
            val t = time * (1.1f + index % 5 * 0.16f) + flake.phase * 6.283f
            flake.entity.components[TransformComponent::class.java]?.apply {
                setPosition(
                    Vector3(
                        flake.x + sin(t) * swirl,
                        y,
                        flake.z + cos(t * 0.83f) * swirl,
                    )
                )
                val pulse = if (index % 6 == 0) 0.72f + (sin(t * 2f) + 1f) * 0.28f else 1f
                setScaleVector(Vector3(pulse))
            }
        }
    }

    fun destroy() = root.destroy()
}

@Composable
fun HomeVolume() {
    val scene = remember { SnowGlobeScene() }
    var storm by remember { mutableStateOf(false) }
    var time by remember { mutableFloatStateOf(0f) }

    DisposableEffect(scene) { onDispose { scene.destroy() } }
    LaunchedEffect(Unit) {
        while (true) {
            delay(16L)
            time += 0.016f
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        SpatialView(
            modifier = Modifier.fillMaxSize(),
            update = { _, _ -> scene.update(time, storm) },
        ) { content, _ ->
            content.addEntity(scene.root)
        }

        Column(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "永恒冰境 · Château de Glace",
                color = PicoTheme.colorScheme.labelPrimary,
                style = PicoTheme.typography.titleMedium,
            )
            Text(
                text = if (storm) "魔法暴风雪环绕着水晶宫殿" else "高精度冰晶建筑在极光雪尘中缓缓旋转",
                color = PicoTheme.colorScheme.labelSecondary,
                style = PicoTheme.typography.bodyMedium,
            )
        }

        Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(onClick = { storm = !storm }) {
                Text(if (storm) "❄ 恢复静谧飘雪" else "✦ 唤醒极光暴风雪")
            }
        }
    }
}
