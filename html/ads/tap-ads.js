(function (root) {
	var config = root.cinderAdConfig || {};
	var rewarded = null;
	var rewardedPending = null;
	var rewardedBusy = false;

	function tapApi() {
		if (typeof tap !== 'undefined' && tap && typeof tap.createRewardedVideoAd === 'function') {
			return tap;
		}
		if (root.tap && typeof root.tap.createRewardedVideoAd === 'function') {
			return root.tap;
		}
		if (typeof GameGlobal !== 'undefined' && GameGlobal && GameGlobal.tap &&
				typeof GameGlobal.tap.createRewardedVideoAd === 'function') {
			return GameGlobal.tap;
		}
		return null;
	}

	function unit(name) {
		var value = config[name];
		return value == null ? '' : String(value);
	}

	function ensureRewarded() {
		var api = tapApi();
		var id = unit('rewardUnit');
		if (!api || !id) return null;
		if (!rewarded) {
			rewarded = api.createRewardedVideoAd({ adUnitId: id });
			rewarded.onClose(function (res) {
				settleRewarded(res && res.isEnded ? 'ended' : 'closed');
			});
		}
		return rewarded;
	}

	function settleRewarded(kind) {
		var pending = rewardedPending;
		rewardedPending = null;
		rewardedBusy = false;
		if (!pending) return;
		if (kind === 'ended' || kind === 'closed') pending.ok(kind);
		else pending.err(kind);
	}

	function showRewarded(onOk, onErr) {
		var ad = ensureRewarded();
		if (!ad) {
			onErr('unavailable');
			return;
		}
		if (rewardedBusy) {
			onErr('busy');
			return;
		}
		rewardedBusy = true;
		rewardedPending = { ok: onOk, err: onErr };
		ad.show().catch(function () {
			return ad.load().then(function () {
				return ad.show();
			});
		}).catch(function () {
			settleRewarded('failed');
		});
	}

	function showInterstitial(onDone) {
		var api = tapApi();
		var id = unit('interstitialUnit');
		if (!api || !id || typeof api.createInterstitialAd !== 'function') {
			onDone('skipped');
			return;
		}
		var done = false;
		function finish(kind) {
			if (done) return;
			done = true;
			onDone(kind);
		}
		var ad;
		try {
			ad = api.createInterstitialAd({ adUnitId: id });
		} catch (e) {
			finish('failed');
			return;
		}
		ad.onClose(function () {
			try { ad.destroy(); } catch (e) {}
			finish('closed');
		});
		ad.show().catch(function () {
			try { ad.destroy(); } catch (e) {}
			finish('failed');
		});
	}

	root.cinderAds = {
		rewardedAvailable: function () {
			return !!ensureRewarded();
		},
		interstitialAvailable: function () {
			var api = tapApi();
			return !!(api && unit('interstitialUnit') && typeof api.createInterstitialAd === 'function');
		},
		showRewarded: showRewarded,
		showInterstitial: showInterstitial
	};
})(typeof window !== 'undefined' ? window : this);
