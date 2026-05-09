package com.sierraespada.wakeywakey.windows.ui

/**
 * Local copies of Material Icons Extended icons used in the app.
 * Avoids the 36 MB material-icons-extended dependency.
 * Source: material-icons-extended-desktop-1.7.3-sources.jar
 */

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

// ── Icons.Filled.CalendarMonth ────────────────────────────────────────────────
val Icons.Filled.CalendarMonth: ImageVector
    get() {
        if (_calendarMonth != null) return _calendarMonth!!
        _calendarMonth = materialIcon(name = "Filled.CalendarMonth") {
            materialPath {
                moveTo(19.0f, 4.0f)
                horizontalLineToRelative(-1.0f)
                verticalLineTo(2.0f)
                horizontalLineToRelative(-2.0f)
                verticalLineToRelative(2.0f)
                horizontalLineTo(8.0f)
                verticalLineTo(2.0f)
                horizontalLineTo(6.0f)
                verticalLineToRelative(2.0f)
                horizontalLineTo(5.0f)
                curveTo(3.89f, 4.0f, 3.01f, 4.9f, 3.01f, 6.0f)
                lineTo(3.0f, 20.0f)
                curveToRelative(0.0f, 1.1f, 0.89f, 2.0f, 2.0f, 2.0f)
                horizontalLineToRelative(14.0f)
                curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                verticalLineTo(6.0f)
                curveTo(21.0f, 4.9f, 20.1f, 4.0f, 19.0f, 4.0f)
                close()
                moveTo(19.0f, 20.0f)
                horizontalLineTo(5.0f)
                verticalLineTo(10.0f)
                horizontalLineToRelative(14.0f)
                verticalLineTo(20.0f)
                close()
                moveTo(9.0f, 14.0f)
                horizontalLineTo(7.0f)
                verticalLineToRelative(-2.0f)
                horizontalLineToRelative(2.0f)
                verticalLineTo(14.0f)
                close()
                moveTo(13.0f, 14.0f)
                horizontalLineToRelative(-2.0f)
                verticalLineToRelative(-2.0f)
                horizontalLineToRelative(2.0f)
                verticalLineTo(14.0f)
                close()
                moveTo(17.0f, 14.0f)
                horizontalLineToRelative(-2.0f)
                verticalLineToRelative(-2.0f)
                horizontalLineToRelative(2.0f)
                verticalLineTo(14.0f)
                close()
                moveTo(9.0f, 18.0f)
                horizontalLineTo(7.0f)
                verticalLineToRelative(-2.0f)
                horizontalLineToRelative(2.0f)
                verticalLineTo(18.0f)
                close()
                moveTo(13.0f, 18.0f)
                horizontalLineToRelative(-2.0f)
                verticalLineToRelative(-2.0f)
                horizontalLineToRelative(2.0f)
                verticalLineTo(18.0f)
                close()
                moveTo(17.0f, 18.0f)
                horizontalLineToRelative(-2.0f)
                verticalLineToRelative(-2.0f)
                horizontalLineToRelative(2.0f)
                verticalLineTo(18.0f)
                close()
            }
        }
        return _calendarMonth!!
    }
private var _calendarMonth: ImageVector? = null

// ── Icons.Filled.CalendarViewMonth ────────────────────────────────────────────
val Icons.Filled.CalendarViewMonth: ImageVector
    get() {
        if (_calendarViewMonth != null) return _calendarViewMonth!!
        _calendarViewMonth = materialIcon(name = "Filled.CalendarViewMonth") {
            materialPath {
                moveTo(20.0f, 4.0f)
                horizontalLineTo(4.0f)
                curveTo(2.9f, 4.0f, 2.0f, 4.9f, 2.0f, 6.0f)
                verticalLineToRelative(12.0f)
                curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                horizontalLineToRelative(16.0f)
                curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                verticalLineTo(6.0f)
                curveTo(22.0f, 4.9f, 21.1f, 4.0f, 20.0f, 4.0f)
                close()
                moveTo(8.0f, 11.0f)
                horizontalLineTo(4.0f)
                verticalLineTo(6.0f)
                horizontalLineToRelative(4.0f)
                verticalLineTo(11.0f)
                close()
                moveTo(14.0f, 11.0f)
                horizontalLineToRelative(-4.0f)
                verticalLineTo(6.0f)
                horizontalLineToRelative(4.0f)
                verticalLineTo(11.0f)
                close()
                moveTo(20.0f, 11.0f)
                horizontalLineToRelative(-4.0f)
                verticalLineTo(6.0f)
                horizontalLineToRelative(4.0f)
                verticalLineTo(11.0f)
                close()
                moveTo(8.0f, 18.0f)
                horizontalLineTo(4.0f)
                verticalLineToRelative(-5.0f)
                horizontalLineToRelative(4.0f)
                verticalLineTo(18.0f)
                close()
                moveTo(14.0f, 18.0f)
                horizontalLineToRelative(-4.0f)
                verticalLineToRelative(-5.0f)
                horizontalLineToRelative(4.0f)
                verticalLineTo(18.0f)
                close()
                moveTo(20.0f, 18.0f)
                horizontalLineToRelative(-4.0f)
                verticalLineToRelative(-5.0f)
                horizontalLineToRelative(4.0f)
                verticalLineTo(18.0f)
                close()
            }
        }
        return _calendarViewMonth!!
    }
private var _calendarViewMonth: ImageVector? = null

// ── Icons.Filled.HourglassTop ─────────────────────────────────────────────────
val Icons.Filled.HourglassTop: ImageVector
    get() {
        if (_hourglassTop != null) return _hourglassTop!!
        _hourglassTop = materialIcon(name = "Filled.HourglassTop") {
            materialPath {
                moveTo(6.0f, 2.0f)
                lineToRelative(0.01f, 6.0f)
                lineTo(10.0f, 12.0f)
                lineToRelative(-3.99f, 4.01f)
                lineTo(6.0f, 22.0f)
                horizontalLineToRelative(12.0f)
                verticalLineToRelative(-6.0f)
                lineToRelative(-4.0f, -4.0f)
                lineToRelative(4.0f, -3.99f)
                verticalLineTo(2.0f)
                horizontalLineTo(6.0f)
                close()
                moveTo(16.0f, 16.5f)
                verticalLineTo(20.0f)
                horizontalLineTo(8.0f)
                verticalLineToRelative(-3.5f)
                lineToRelative(4.0f, -4.0f)
                lineTo(16.0f, 16.5f)
                close()
            }
        }
        return _hourglassTop!!
    }
private var _hourglassTop: ImageVector? = null

// ── Icons.Filled.Dvr ──────────────────────────────────────────────────────────
@Suppress("DEPRECATION")
val Icons.Filled.Dvr: ImageVector
    get() {
        if (_dvr != null) return _dvr!!
        _dvr = materialIcon(name = "Filled.Dvr") {
            materialPath {
                moveTo(21.0f, 3.0f)
                lineTo(3.0f, 3.0f)
                curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
                verticalLineToRelative(12.0f)
                curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                horizontalLineToRelative(5.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(8.0f)
                verticalLineToRelative(-2.0f)
                horizontalLineToRelative(5.0f)
                curveToRelative(1.1f, 0.0f, 1.99f, -0.9f, 1.99f, -2.0f)
                lineTo(23.0f, 5.0f)
                curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
                close()
                moveTo(21.0f, 17.0f)
                lineTo(3.0f, 17.0f)
                lineTo(3.0f, 5.0f)
                horizontalLineToRelative(18.0f)
                verticalLineToRelative(12.0f)
                close()
                moveTo(19.0f, 8.0f)
                lineTo(8.0f, 8.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(11.0f)
                lineTo(19.0f, 8.0f)
                close()
                moveTo(19.0f, 12.0f)
                lineTo(8.0f, 12.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(11.0f)
                verticalLineToRelative(-2.0f)
                close()
                moveTo(7.0f, 8.0f)
                lineTo(5.0f, 8.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(2.0f)
                lineTo(7.0f, 8.0f)
                close()
                moveTo(7.0f, 12.0f)
                lineTo(5.0f, 12.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(2.0f)
                verticalLineToRelative(-2.0f)
                close()
            }
        }
        return _dvr!!
    }
private var _dvr: ImageVector? = null

// ── Icons.Filled.LinkOff ──────────────────────────────────────────────────────
val Icons.Filled.LinkOff: ImageVector
    get() {
        if (_linkOff != null) return _linkOff!!
        _linkOff = materialIcon(name = "Filled.LinkOff") {
            materialPath {
                moveTo(17.0f, 7.0f)
                horizontalLineToRelative(-4.0f)
                verticalLineToRelative(1.9f)
                horizontalLineToRelative(4.0f)
                curveToRelative(1.71f, 0.0f, 3.1f, 1.39f, 3.1f, 3.1f)
                curveToRelative(0.0f, 1.43f, -0.98f, 2.63f, -2.31f, 2.98f)
                lineToRelative(1.46f, 1.46f)
                curveTo(20.88f, 15.61f, 22.0f, 13.95f, 22.0f, 12.0f)
                curveToRelative(0.0f, -2.76f, -2.24f, -5.0f, -5.0f, -5.0f)
                close()
                moveTo(16.0f, 11.0f)
                horizontalLineToRelative(-2.19f)
                lineToRelative(2.0f, 2.0f)
                lineTo(16.0f, 13.0f)
                close()
                moveTo(2.0f, 4.27f)
                lineToRelative(3.11f, 3.11f)
                curveTo(3.29f, 8.12f, 2.0f, 9.91f, 2.0f, 12.0f)
                curveToRelative(0.0f, 2.76f, 2.24f, 5.0f, 5.0f, 5.0f)
                horizontalLineToRelative(4.0f)
                verticalLineToRelative(-1.9f)
                lineTo(7.0f, 15.1f)
                curveToRelative(-1.71f, 0.0f, -3.1f, -1.39f, -3.1f, -3.1f)
                curveToRelative(0.0f, -1.59f, 1.21f, -2.9f, 2.76f, -3.07f)
                lineTo(8.73f, 11.0f)
                lineTo(8.0f, 11.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(2.73f)
                lineTo(13.0f, 15.27f)
                lineTo(13.0f, 17.0f)
                horizontalLineToRelative(1.73f)
                lineToRelative(4.01f, 4.0f)
                lineTo(20.0f, 19.74f)
                lineTo(3.27f, 3.0f)
                lineTo(2.0f, 4.27f)
                close()
            }
        }
        return _linkOff!!
    }
private var _linkOff: ImageVector? = null

// ── Icons.Filled.BugReport ────────────────────────────────────────────────────
val Icons.Filled.BugReport: ImageVector
    get() {
        if (_bugReport != null) return _bugReport!!
        _bugReport = materialIcon(name = "Filled.BugReport") {
            materialPath {
                moveTo(20.0f, 8.0f)
                horizontalLineToRelative(-2.81f)
                curveToRelative(-0.45f, -0.78f, -1.07f, -1.45f, -1.82f, -1.96f)
                lineTo(17.0f, 4.41f)
                lineTo(15.59f, 3.0f)
                lineToRelative(-2.17f, 2.17f)
                curveTo(12.96f, 5.06f, 12.49f, 5.0f, 12.0f, 5.0f)
                curveToRelative(-0.49f, 0.0f, -0.96f, 0.06f, -1.41f, 0.17f)
                lineTo(8.41f, 3.0f)
                lineTo(7.0f, 4.41f)
                lineToRelative(1.62f, 1.63f)
                curveTo(7.88f, 6.55f, 7.26f, 7.22f, 6.81f, 8.0f)
                lineTo(4.0f, 8.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(2.09f)
                curveToRelative(-0.05f, 0.33f, -0.09f, 0.66f, -0.09f, 1.0f)
                verticalLineToRelative(1.0f)
                lineTo(4.0f, 12.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(2.0f)
                verticalLineToRelative(1.0f)
                curveToRelative(0.0f, 0.34f, 0.04f, 0.67f, 0.09f, 1.0f)
                lineTo(4.0f, 16.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(2.81f)
                curveToRelative(1.04f, 1.79f, 2.97f, 3.0f, 5.19f, 3.0f)
                reflectiveCurveToRelative(4.15f, -1.21f, 5.19f, -3.0f)
                lineTo(20.0f, 18.0f)
                verticalLineToRelative(-2.0f)
                horizontalLineToRelative(-2.09f)
                curveToRelative(0.05f, -0.33f, 0.09f, -0.66f, 0.09f, -1.0f)
                verticalLineToRelative(-1.0f)
                horizontalLineToRelative(2.0f)
                verticalLineToRelative(-2.0f)
                horizontalLineToRelative(-2.0f)
                verticalLineToRelative(-1.0f)
                curveToRelative(0.0f, -0.34f, -0.04f, -0.67f, -0.09f, -1.0f)
                lineTo(20.0f, 10.0f)
                lineTo(20.0f, 8.0f)
                close()
                moveTo(14.0f, 16.0f)
                horizontalLineToRelative(-4.0f)
                verticalLineToRelative(-2.0f)
                horizontalLineToRelative(4.0f)
                verticalLineTo(16.0f)
                close()
                moveTo(14.0f, 12.0f)
                horizontalLineToRelative(-4.0f)
                verticalLineToRelative(-2.0f)
                horizontalLineToRelative(4.0f)
                verticalLineTo(12.0f)
                close()
            }
        }
        return _bugReport!!
    }
private var _bugReport: ImageVector? = null

// ── Icons.Filled.Alarm ────────────────────────────────────────────────────────
val Icons.Filled.Alarm: ImageVector
    get() {
        if (_alarm != null) return _alarm!!
        _alarm = materialIcon(name = "Filled.Alarm") {
            materialPath {
                moveTo(22.0f, 5.72f); lineToRelative(-4.6f, -3.86f); lineToRelative(-1.29f, 1.53f)
                lineToRelative(4.6f, 3.86f); lineTo(22.0f, 5.72f); close()
                moveTo(7.88f, 3.39f); lineTo(6.6f, 1.86f); lineTo(2.0f, 5.71f)
                lineToRelative(1.29f, 1.53f); lineToRelative(4.59f, -3.85f); close()
                moveTo(12.5f, 8.0f); lineTo(11.0f, 8.0f); verticalLineToRelative(6.0f)
                lineToRelative(4.75f, 2.85f); lineToRelative(0.75f, -1.23f)
                lineToRelative(-4.0f, -2.37f); lineTo(12.5f, 8.0f); close()
                moveTo(12.0f, 4.0f)
                curveToRelative(-4.97f, 0.0f, -9.0f, 4.03f, -9.0f, 9.0f)
                reflectiveCurveToRelative(4.02f, 9.0f, 9.0f, 9.0f)
                curveToRelative(4.97f, 0.0f, 9.0f, -4.03f, 9.0f, -9.0f)
                reflectiveCurveToRelative(-4.03f, -9.0f, -9.0f, -9.0f); close()
                moveTo(12.0f, 20.0f)
                curveToRelative(-3.87f, 0.0f, -7.0f, -3.13f, -7.0f, -7.0f)
                reflectiveCurveToRelative(3.13f, -7.0f, 7.0f, -7.0f)
                reflectiveCurveToRelative(7.0f, 3.13f, 7.0f, 7.0f)
                reflectiveCurveToRelative(-3.13f, 7.0f, -7.0f, 7.0f); close()
            }
        }
        return _alarm!!
    }
private var _alarm: ImageVector? = null

// ── Icons.Filled.Videocam ─────────────────────────────────────────────────────
val Icons.Filled.Videocam: ImageVector
    get() {
        if (_videocam != null) return _videocam!!
        _videocam = materialIcon(name = "Filled.Videocam") {
            materialPath {
                moveTo(17.0f, 10.5f); verticalLineTo(7.0f)
                curveToRelative(0.0f, -0.55f, -0.45f, -1.0f, -1.0f, -1.0f)
                horizontalLineTo(4.0f); curveToRelative(-0.55f, 0.0f, -1.0f, 0.45f, -1.0f, 1.0f)
                verticalLineToRelative(10.0f); curveToRelative(0.0f, 0.55f, 0.45f, 1.0f, 1.0f, 1.0f)
                horizontalLineToRelative(12.0f); curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
                verticalLineToRelative(-3.5f); lineToRelative(4.0f, 4.0f); verticalLineToRelative(-11.0f)
                lineToRelative(-4.0f, 4.0f); close()
            }
        }
        return _videocam!!
    }
private var _videocam: ImageVector? = null

// ── Icons.Filled.SwapHoriz ────────────────────────────────────────────────────
val Icons.Filled.SwapHoriz: ImageVector
    get() {
        if (_swapHoriz != null) return _swapHoriz!!
        _swapHoriz = materialIcon(name = "Filled.SwapHoriz") {
            materialPath {
                moveTo(6.99f, 11.0f); lineTo(3.0f, 15.0f); lineToRelative(3.99f, 4.0f)
                verticalLineToRelative(-3.0f); horizontalLineTo(14.0f); verticalLineToRelative(-2.0f)
                horizontalLineTo(6.99f); verticalLineToRelative(-3.0f); close()
                moveTo(21.0f, 9.0f); lineToRelative(-3.99f, -4.0f); verticalLineToRelative(3.0f)
                horizontalLineTo(10.0f); verticalLineToRelative(2.0f); horizontalLineToRelative(7.01f)
                verticalLineToRelative(3.0f); lineTo(21.0f, 9.0f); close()
            }
        }
        return _swapHoriz!!
    }
private var _swapHoriz: ImageVector? = null

// ── Icons.Filled.FilterList ───────────────────────────────────────────────────
val Icons.Filled.FilterList: ImageVector
    get() {
        if (_filterList != null) return _filterList!!
        _filterList = materialIcon(name = "Filled.FilterList") {
            materialPath {
                moveTo(10.0f, 18.0f); horizontalLineToRelative(4.0f); verticalLineToRelative(-2.0f)
                horizontalLineToRelative(-4.0f); verticalLineToRelative(2.0f); close()
                moveTo(3.0f, 6.0f); verticalLineToRelative(2.0f); horizontalLineToRelative(18.0f)
                lineTo(21.0f, 6.0f); lineTo(3.0f, 6.0f); close()
                moveTo(6.0f, 13.0f); horizontalLineToRelative(12.0f); verticalLineToRelative(-2.0f)
                lineTo(6.0f, 11.0f); verticalLineToRelative(2.0f); close()
            }
        }
        return _filterList!!
    }
private var _filterList: ImageVector? = null

// ── Icons.Filled.VolumeUp ─────────────────────────────────────────────────────
@Suppress("DEPRECATION")
val Icons.Filled.VolumeUp: ImageVector
    get() {
        if (_volumeUp != null) return _volumeUp!!
        _volumeUp = materialIcon(name = "Filled.VolumeUp") {
            materialPath {
                moveTo(3.0f, 9.0f); verticalLineToRelative(6.0f); horizontalLineToRelative(4.0f)
                lineToRelative(5.0f, 5.0f); lineTo(12.0f, 4.0f); lineTo(7.0f, 9.0f); lineTo(3.0f, 9.0f); close()
                moveTo(16.5f, 12.0f)
                curveToRelative(0.0f, -1.77f, -1.02f, -3.29f, -2.5f, -4.03f)
                verticalLineToRelative(8.05f)
                curveToRelative(1.48f, -0.73f, 2.5f, -2.25f, 2.5f, -4.02f); close()
                moveTo(14.0f, 3.23f); verticalLineToRelative(2.06f)
                curveToRelative(2.89f, 0.86f, 5.0f, 3.54f, 5.0f, 6.71f)
                reflectiveCurveToRelative(-2.11f, 5.85f, -5.0f, 6.71f); verticalLineToRelative(2.06f)
                curveToRelative(4.01f, -0.91f, 7.0f, -4.49f, 7.0f, -8.77f)
                reflectiveCurveToRelative(-2.99f, -7.86f, -7.0f, -8.77f); close()
            }
        }
        return _volumeUp!!
    }
private var _volumeUp: ImageVector? = null

// ── Icons.Filled.VolumeDown ───────────────────────────────────────────────────
@Suppress("DEPRECATION")
val Icons.Filled.VolumeDown: ImageVector
    get() {
        if (_volumeDown != null) return _volumeDown!!
        _volumeDown = materialIcon(name = "Filled.VolumeDown") {
            materialPath {
                moveTo(18.5f, 12.0f)
                curveToRelative(0.0f, -1.77f, -1.02f, -3.29f, -2.5f, -4.03f)
                verticalLineToRelative(8.05f)
                curveToRelative(1.48f, -0.73f, 2.5f, -2.25f, 2.5f, -4.02f); close()
                moveTo(5.0f, 9.0f); verticalLineToRelative(6.0f); horizontalLineToRelative(4.0f)
                lineToRelative(5.0f, 5.0f); verticalLineTo(4.0f); lineTo(9.0f, 9.0f); horizontalLineTo(5.0f); close()
            }
        }
        return _volumeDown!!
    }
private var _volumeDown: ImageVector? = null

// ── Icons.Filled.Schedule ─────────────────────────────────────────────────────
val Icons.Filled.Schedule: ImageVector
    get() {
        if (_schedule != null) return _schedule!!
        _schedule = materialIcon(name = "Filled.Schedule") {
            materialPath {
                moveTo(11.99f, 2.0f)
                curveTo(6.47f, 2.0f, 2.0f, 6.48f, 2.0f, 12.0f)
                reflectiveCurveToRelative(4.47f, 10.0f, 9.99f, 10.0f)
                curveTo(17.52f, 22.0f, 22.0f, 17.52f, 22.0f, 12.0f)
                reflectiveCurveTo(17.52f, 2.0f, 11.99f, 2.0f); close()
                moveTo(12.0f, 20.0f)
                curveToRelative(-4.42f, 0.0f, -8.0f, -3.58f, -8.0f, -8.0f)
                reflectiveCurveToRelative(3.58f, -8.0f, 8.0f, -8.0f)
                reflectiveCurveToRelative(8.0f, 3.58f, 8.0f, 8.0f)
                reflectiveCurveToRelative(-3.58f, 8.0f, -8.0f, 8.0f); close()
            }
            materialPath {
                moveTo(12.5f, 7.0f); horizontalLineTo(11.0f); verticalLineToRelative(6.0f)
                lineToRelative(5.25f, 3.15f); lineToRelative(0.75f, -1.23f); lineToRelative(-4.5f, -2.67f); close()
            }
        }
        return _schedule!!
    }
private var _schedule: ImageVector? = null

// ── Icons.Filled.ExpandMore ───────────────────────────────────────────────────
val Icons.Filled.ExpandMore: ImageVector
    get() {
        if (_expandMore != null) return _expandMore!!
        _expandMore = materialIcon(name = "Filled.ExpandMore") {
            materialPath {
                moveTo(16.59f, 8.59f); lineTo(12.0f, 13.17f); lineTo(7.41f, 8.59f)
                lineTo(6.0f, 10.0f); lineToRelative(6.0f, 6.0f); lineToRelative(6.0f, -6.0f); close()
            }
        }
        return _expandMore!!
    }
private var _expandMore: ImageVector? = null

// ── Icons.Filled.Palette ──────────────────────────────────────────────────────
val Icons.Filled.Palette: ImageVector
    get() {
        if (_palette != null) return _palette!!
        _palette = materialIcon(name = "Filled.Palette") {
            materialPath {
                moveTo(12.0f, 2.0f)
                curveTo(6.49f, 2.0f, 2.0f, 6.49f, 2.0f, 12.0f)
                reflectiveCurveToRelative(4.49f, 10.0f, 10.0f, 10.0f)
                curveToRelative(1.38f, 0.0f, 2.5f, -1.12f, 2.5f, -2.5f)
                curveToRelative(0.0f, -0.61f, -0.23f, -1.2f, -0.64f, -1.67f)
                curveToRelative(-0.08f, -0.1f, -0.13f, -0.21f, -0.13f, -0.33f)
                curveToRelative(0.0f, -0.28f, 0.22f, -0.5f, 0.5f, -0.5f)
                horizontalLineTo(16.0f)
                curveToRelative(3.31f, 0.0f, 6.0f, -2.69f, 6.0f, -6.0f)
                curveTo(22.0f, 6.04f, 17.51f, 2.0f, 12.0f, 2.0f); close()
                moveTo(17.5f, 13.0f)
                curveToRelative(-0.83f, 0.0f, -1.5f, -0.67f, -1.5f, -1.5f)
                curveToRelative(0.0f, -0.83f, 0.67f, -1.5f, 1.5f, -1.5f)
                reflectiveCurveToRelative(1.5f, 0.67f, 1.5f, 1.5f)
                curveTo(19.0f, 12.33f, 18.33f, 13.0f, 17.5f, 13.0f); close()
                moveTo(14.5f, 9.0f)
                curveTo(13.67f, 9.0f, 13.0f, 8.33f, 13.0f, 7.5f)
                curveTo(13.0f, 6.67f, 13.67f, 6.0f, 14.5f, 6.0f)
                reflectiveCurveTo(16.0f, 6.67f, 16.0f, 7.5f)
                curveTo(16.0f, 8.33f, 15.33f, 9.0f, 14.5f, 9.0f); close()
                moveTo(5.0f, 11.5f)
                curveTo(5.0f, 10.67f, 5.67f, 10.0f, 6.5f, 10.0f)
                reflectiveCurveTo(8.0f, 10.67f, 8.0f, 11.5f)
                curveTo(8.0f, 12.33f, 7.33f, 13.0f, 6.5f, 13.0f)
                reflectiveCurveTo(5.0f, 12.33f, 5.0f, 11.5f); close()
                moveTo(11.0f, 7.5f)
                curveTo(11.0f, 8.33f, 10.33f, 9.0f, 9.5f, 9.0f)
                reflectiveCurveTo(8.0f, 8.33f, 8.0f, 7.5f)
                curveTo(8.0f, 6.67f, 8.67f, 6.0f, 9.5f, 6.0f)
                reflectiveCurveTo(11.0f, 6.67f, 11.0f, 7.5f); close()
            }
        }
        return _palette!!
    }
private var _palette: ImageVector? = null
