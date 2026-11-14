require('./adapter/text-codec.js');
require('./adapter/weapp-adapter.js');

if (typeof define === 'function') {
	try {
		define.amd = undefined;
	} catch (err) {
	}
}

wx.showLoading({ title: '加载游戏', mask: true });

wx.loadSubpackage({
	name: 'engine',
	success() {
		try {
			require('./engine/scripts/freetype.js');
			require('./engine/scripts/gdx.wasm.js');
			require('./engine/scripts/howler.js');
			const game = require('./engine/game.js');
			wx.hideLoading();
			if (game && typeof game.main === 'function') {
				game.main();
			} else if (typeof GameGlobal.main === 'function') {
				GameGlobal.main();
			} else {
				throw new Error('TeaVM main() missing');
			}
		} catch (err) {
			wx.hideLoading();
			console.error(err);
			wx.showModal({
				title: '启动失败',
				content: String(err && err.stack ? err.stack : err).slice(0, 800),
				showCancel: false
			});
		}
	},
	fail(err) {
		wx.hideLoading();
		console.error(err);
		wx.showModal({
			title: '分包加载失败',
			content: String(err && err.errMsg ? err.errMsg : err),
			showCancel: false
		});
	}
});
