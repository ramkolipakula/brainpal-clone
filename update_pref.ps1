$adb = "D:\tools\android-sdk\platform-tools\adb.exe"
$xml = @"
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <int name="daily_reel_limit" value="100" />
    <int name="daily_limit_mins" value="180" />
    <string name="last_active_date">2026-11-03</string>
    <boolean name="strict_mode" value="true" />
    <int name="emergency_passes_left" value="3" />
    <boolean name="onboarding_done" value="true" />
    <string name="floating_bubble_size">large</string>
    <long name="snooze_until" value="1793625899088" />
    <int name="reels_today_2026-11-03" value="43" />
    <set name="blocked_packages">
        <string>com.zhiliaoapp.musically</string>
        <string>com.snapchat.android</string>
        <string>com.google.android.youtube</string>
        <string>com.instagram.android</string>
    </set>
    <boolean name="floating_bubble_enabled" value="true" />
</map>
"@

$xml | & $adb shell "run-as com.brainrot.rebuilt tee shared_prefs/brainpal_prefs.xml"
& $adb shell am force-stop com.brainrot.rebuilt
& $adb shell settings --user 0 put secure enabled_accessibility_services com.brainrot.rebuilt/com.brainrot.rebuilt.service.ReelsAccessibilityService
& $adb shell settings --user 0 put secure accessibility_enabled 1
& $adb shell am start -n com.brainrot.rebuilt/com.brainrot.rebuilt.MainActivity

