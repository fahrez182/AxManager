#!/system/bin/sh
# G1 — GMS deep component disable
# Source: microg/GmsCore issues, XDA, MrCarb0n
# Disables the internal components of com.google.android.gms that
# are the main wakeup sources. GMS app itself stays installed,
# so FCM still works (combine with G8 for safety).
# Reversible: G1 reverts all listed components.
MODULE_ID="G1_gms_deep_disable"
. "$(dirname "$0")/../lib/plus_core.sh"
vopl_init

COMPONENTS="
com.google.android.gms/.chimera.GmsCoreContainerService
com.google.android.gms/.chimera.PersistentApiService
com.google.android.gms/com.google.android.gms.chimera.GmsApiService
com.google.android.gms/com.google.android.gms.analytics.AnalyticsService
com.google.android.gms/com.google.android.gms.measurement.AppMeasurementService
com.google.android.gms/com.google.android.gms.checkin.CheckinService
com.google.android.gms/com.google.android.gms.checkin.CheckinApiService
com.google.android.gms/com.google.android.gms.gcm.GcmService
com.google.android.gms/com.google.android.gms.gcm.GcmIntentService
com.google.android.gms/com.google.android.gms.gcm.NMSTaskService
com.google.android.gms/com.google.android.gms.auth.be.proximity.authorization.broadcastreceiver.PhenotypeBroadcastReceiver
com.google.android.gms/com.google.android.gms.nearby.bootstrap.service.NearbyBootstrapService
com.google.android.gms/com.google.android.gms.update.SystemUpdateService
com.google.android.gms/com.google.android.gms.ads.social.GoogleAdMobPersistentService
com.google.android.gms/com.google.android.gms.ads.measurement.GclidListenerService
com.google.android.gms/com.google.android.gms.ads.jams.BandwidthListenerService
com.google.android.gms/com.google.android.gms.locationsharingreporter.LocationSharingReporterService
com.google.android.gms/com.google.android.gms.people.pub.PeopleService
com.google.android.gms/com.google.android.gms.fitness.service.FitnessSensorService
com.google.android.gms/com.google.android.gms.fitness.service.ble.FitnessBleService
com.google.android.gms/com.google.android.gms.fitness.service.config.FitnessConfigService
com.google.android.gms/com.google.android.gms.auth.cryptauth.AuthCryptauthService
com.google.android.gms/com.google.android.gms.tapandpay.service.TapAndPayService
com.google.android.gms/com.google.android.gms.trustagent.TrustAgentService
com.google.android.gms/com.google.android.gms.auth.trustagent.GoogleTrustAgent
com.google.android.gms/com.google.android.gms.drive.sync.receiver.SyncReceiverService
com.google.android.gms/com.google.android.gms.drive.metadata.sync.receiver.MetadataSyncService
com.google.android.gms/com.google.android.gms.nearby.messages.service.NearbyMessagesService
com.google.android.gms/com.google.android.gms.lockbox.LockboxService
com.google.android.gms/com.google.android.gms.herrevad.services.ImagingService
com.google.android.gms/com.google.android.gms.herrevad.services.NetworkQualityService
com.google.android.gms/com.google.android.gms.herrevad.services.UsageReportingService
com.google.android.gms/com.google.android.gms.config.services.ConfigurationFetcherService
com.google.android.gms/com.google.android.gms.config.ConfigService
com.google.android.gms/com.google.android.gms.security.recovery.PasswordResetService
com.google.android.gms/com.google.android.gms.backup.BackupTransportService
com.google.android.gms/com.google.android.gms.wearable.service.WearableService
com.google.android.gms/com.google.android.gms.wallet.service.PersistentPaymentService
com.google.android.gms/com.google.android.gms.accountsettings.MarketingContentSyncService
com.google.android.gms/com.google.android.gms.accountsettings.ui.PrivateContentSyncService
com.google.android.gms/com.google.android.gms.ads.social.GoogleImaAdService
com.google.android.gms/com.google.android.gms.games.service.GamesIntentService
com.google.android.gms/com.google.android.gms.games.service.PlayGamesNearbyService
com.google.android.gms/com.google.android.gms.people.pub.PeopleBroadcastReceiver
com.google.android.gms/com.google.android.gms.auth.managed.ui.SilentAuthBroadcastReceiver
com.google.android.gms/com.google.android.gms.udc.service.UdcApiService
com.google.android.gms/com.google.android.gms.notifications.service.MessagingNotificationListenerService
"

apply() {
    vopl_apply_or_skip G1_GMS_DEEP_DISABLE || return 0
    local pkg="com.google.android.gms"
    vopl_pkg_exists "$pkg" || { vopl_log WARN "GMS not installed"; return 0; }
    local c=0
    for comp in $COMPONENTS; do
        if vopl_component_exists "$pkg" "$comp"; then
            vopl_run "$MODULE_ID: $comp" \
                pm disable "$pkg/$comp"
            c=$((c + 1))
        fi
    done
    vopl_log INFO "$MODULE_ID: disabled $c components"
}

revert() {
    local pkg="com.google.android.gms"
    vopl_pkg_exists "$pkg" || return 0
    for comp in $COMPONENTS; do
        if vopl_component_exists "$pkg" "$comp"; then
            vopl_run "revert $MODULE_ID" pm enable "$pkg/$comp"
        fi
    done
    vopl_log INFO "$MODULE_ID: re-enabled all"
}

case "${1:-apply}" in apply) apply ;; revert) revert ;; *) exit 1 ;; esac
