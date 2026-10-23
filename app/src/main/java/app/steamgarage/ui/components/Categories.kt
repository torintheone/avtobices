package app.steamgarage.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.steamgarage.R
import app.steamgarage.domain.Category
import app.steamgarage.ui.theme.Steam

@get:DrawableRes
val Category.icon: Int
    get() = when (this) {
        Category.OILS -> R.drawable.ic_cat_oils
        Category.FILTERS -> R.drawable.ic_cat_filters
        Category.FLUIDS -> R.drawable.ic_cat_fluids
        Category.IGNITION -> R.drawable.ic_cat_ignition
        Category.BELTS -> R.drawable.ic_cat_belts
        Category.BRAKES -> R.drawable.ic_cat_brakes
        Category.TIRES -> R.drawable.ic_cat_tires
        Category.ELECTRICS -> R.drawable.ic_cat_electrics
        Category.OTHER -> R.drawable.ic_cat_other
    }

@get:StringRes
val Category.label: Int
    get() = when (this) {
        Category.OILS -> R.string.category_oils
        Category.FILTERS -> R.string.category_filters
        Category.FLUIDS -> R.string.category_fluids
        Category.IGNITION -> R.string.category_ignition
        Category.BELTS -> R.string.category_belts
        Category.BRAKES -> R.string.category_brakes
        Category.TIRES -> R.string.category_tires
        Category.ELECTRICS -> R.string.category_electrics
        Category.OTHER -> R.string.category_other
    }

/** Цвет металла таблички категории; гравированная иконка на нём читается в обеих темах. */
val Category.plateColor: Color
    get() = when (this) {
        Category.OILS -> Color(0xFFC9A227)
        Category.FILTERS -> Color(0xFFA8A9AD)
        Category.FLUIDS -> Color(0xFF6FA8A0)
        Category.IGNITION -> Color(0xFFC46A3A)
        Category.BELTS -> Color(0xFF9C7A55)
        Category.BRAKES -> Color(0xFFB85C4A)
        Category.TIRES -> Color(0xFF8C8C8C)
        Category.ELECTRICS -> Color(0xFFD4B23C)
        Category.OTHER -> Color(0xFFB08D57)
    }

/** Табличка категории: металлическая плашка с гравированной иконкой. */
@Composable
fun CategoryPlate(category: Category, modifier: Modifier = Modifier, size: Dp = 36.dp) {
    val palette = Steam.palette
    val base = category.plateColor
    val shape = CutCornerShape(size * 0.18f)
    Box(
        modifier = modifier
            .size(size)
            .background(Brush.linearGradient(listOf(lerp(base, Color.White, 0.25f), base, lerp(base, Color.Black, 0.25f))), shape)
            .border(1.dp, lerp(base, Color.Black, 0.45f), shape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(category.icon),
            contentDescription = null,
            tint = palette.engraved,
            modifier = Modifier.size(size * 0.66f),
        )
    }
}

/** Цвета эмалевого значка машины. Номер цвета хранится в машине. */
val CarColors: List<Color> = listOf(
    Color(0xFFC9A94E), // латунь
    Color(0xFFB87333), // медь
    Color(0xFF43B3AE), // патина
    Color(0xFFA4161A), // красная эмаль
    Color(0xFF4A6C8C), // синяя сталь
    Color(0xFF2F5D3A), // бутылочное стекло
    Color(0xFF6B4226), // орех
    Color(0xFF5E6164), // железо
)

/** Эмалевый значок машины: цветной круг в латунной оправе. */
@Composable
fun CarBadge(color: Int, modifier: Modifier = Modifier, size: Dp = 22.dp) {
    val palette = Steam.palette
    val fill = CarColors[color.mod(CarColors.size)]
    Canvas(modifier.size(size)) {
        val r = this.size.minDimension / 2f
        drawCircle(Brush.linearGradient(listOf(palette.brassLight, palette.brassShadow)), r, center)
        drawCircle(
            Brush.radialGradient(listOf(lerp(fill, Color.White, 0.35f), fill, lerp(fill, Color.Black, 0.3f)), center - Offset(r * 0.25f, r * 0.25f), r),
            r * 0.78f,
            center,
        )
        drawCircle(Color.White.copy(alpha = 0.35f), r * 0.78f, center, style = Stroke(width = r * 0.06f))
    }
}
