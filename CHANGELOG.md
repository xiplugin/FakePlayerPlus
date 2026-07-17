# Changelog

## [1.6.2](https://github.com/xiplugin/FakePlayerPlus/compare/v1.6.1...v1.6.2) (2026-07-17)


### Bug Fixes

* change some Player commands to CommandSender ([6e1c67c](https://github.com/xiplugin/FakePlayerPlus/commit/6e1c67c021b09b27ff55ad015a4002724960038d))

## [1.6.1](https://github.com/xiplugin/FakePlayerPlus/compare/v1.6.0...v1.6.1) (2026-07-13)


### Bug Fixes

* the inventory be emptied when spawning fakeplayer ([7fe3d9e](https://github.com/xiplugin/FakePlayerPlus/commit/7fe3d9e77db52314d6037154e2fa74a943124105))

## [1.6.0](https://github.com/xiplugin/FakePlayerPlus/compare/v1.5.0...v1.6.0) (2026-07-04)


### Features

* overhaul scheduler implementation and add Folia support ([e0416b8](https://github.com/xiplugin/FakePlayerPlus/commit/e0416b8a51e8b4659d7283db2bf884db786fbf39))

## [1.5.0](https://github.com/xiplugin/FakePlayerPlus/compare/v1.4.3...v1.5.0) (2026-07-01)


### Features

* more placeholder vars ([4c19b6e](https://github.com/xiplugin/FakePlayerPlus/commit/4c19b6e31c4a267aaa54841aae5573f05c264f7e))
* sort fakeplayer list by spawnTime and optimize concurrent read/write ([e41497b](https://github.com/xiplugin/FakePlayerPlus/commit/e41497ba7c1d26051a19e3866e1b05a7513fffb3))


### Bug Fixes

* clear pending queue when fakeplayer login is denied ([611c38a](https://github.com/xiplugin/FakePlayerPlus/commit/611c38ab84f18d52883a34593935552001fab0d3))

## [1.4.3](https://github.com/xiplugin/FakePlayerPlus/compare/v1.4.2...v1.4.3) (2026-07-01)


### Bug Fixes

* admin cannot force spawn ([fedd442](https://github.com/xiplugin/FakePlayerPlus/commit/fedd442b7b91866a359ba8b967c1a673cdb11405))
* fakeplayer fail to follow-quit when spawned by admin but owned by others ([daf4b19](https://github.com/xiplugin/FakePlayerPlus/commit/daf4b19665e3d832caf2ffdb7e10b0607ff3d23a))

## [1.4.2](https://github.com/xiplugin/FakePlayerPlus/compare/v1.4.1...v1.4.2) (2026-06-30)


### Bug Fixes

* fake players unable to be kicked in 26.1.1+ by using `/kick` or `/fp remove` ([4f9e196](https://github.com/xiplugin/FakePlayerPlus/commit/4f9e19663f331ff58bcd98aca8eabc7b991f10ad))

## [1.4.1](https://github.com/xiplugin/FakePlayerPlus/compare/v1.4.0...v1.4.1) (2026-06-30)


### Bug Fixes

* fix severe CPU and RAM spikes from Lamp @Flag and custom ParameterType conflict ([ab356ec](https://github.com/xiplugin/FakePlayerPlus/commit/ab356ec8438f08325c43603e273720257bdbaf32))
* prevent duplicate fakeplayer spawning tasks ([756d028](https://github.com/xiplugin/FakePlayerPlus/commit/756d02833f77222a2dd0becdb1fce6814ba782c4))

## [1.4.0](https://github.com/xiplugin/FakePlayerPlus/compare/v1.3.1...v1.4.0) (2026-06-29)


### Features

* save exp on death and add expme command ([7045f7e](https://github.com/xiplugin/FakePlayerPlus/commit/7045f7e2632e466ecec664ecb8dbe4eab194df9e))


### Performance Improvements

* avoid redundant metadata and address creation per channel ([9ed8b63](https://github.com/xiplugin/FakePlayerPlus/commit/9ed8b63831337b0facc820ade1e6a438c5202c46))
* remove redundant player.isOnline check in fake player ticker ([2d47298](https://github.com/xiplugin/FakePlayerPlus/commit/2d47298cbb2f0ca437260daa5311e43dc919336b))

## [1.3.1](https://github.com/xiplugin/FakePlayerPlus/compare/v1.3.0...v1.3.1) (2026-06-29)


### Bug Fixes

* **security:** patch item duplication exploit via keepInventory ([a923591](https://github.com/xiplugin/FakePlayerPlus/commit/a9235916d2eb4b9208ba3bd37ccbd2776c5df2d2))

## [1.3.0](https://github.com/xiplugin/FakePlayerPlus/compare/v1.2.0...v1.3.0) (2026-06-28)


### Features

* add keep inventory config option ([d3f1c01](https://github.com/xiplugin/FakePlayerPlus/commit/d3f1c01dcf355321a18aabcfa6784002f8840b6f))
* add prevent kicking option for fake players to enhance compatibility ([08860ca](https://github.com/xiplugin/FakePlayerPlus/commit/08860ca939ba1d657533825dc98bf6b21a31ad0f))


### Bug Fixes

* call AsyncPlayerPreLoginEvent before spawn ([d31c0a1](https://github.com/xiplugin/FakePlayerPlus/commit/d31c0a10de2c98ee03a843384c906f75818e2d30))
* ensure quitting on spawn failure runs on the main thread ([6deef83](https://github.com/xiplugin/FakePlayerPlus/commit/6deef83dd28c0b3eaa1993a53a1d65c71fbbc8ff))

## [1.2.0](https://github.com/xiplugin/FakePlayerPlus/compare/v1.1.4...v1.2.0) (2026-06-28)


### Features

* implement fakeplayer default skin option ([54e94ac](https://github.com/xiplugin/FakePlayerPlus/commit/54e94ac92addfea1861b4b996613ea4b02e4ffef))


### Bug Fixes

* admin cannot select unowned fakeplayer ([5835328](https://github.com/xiplugin/FakePlayerPlus/commit/583532802a2e88c26f4d0c71b154720d6274bdd9))
* unable to remove fake players in 26.2 ([c916968](https://github.com/xiplugin/FakePlayerPlus/commit/c9169682604a4880dfeaf415eb2f9739e7e613e8))

## [1.1.4](https://github.com/xiplugin/FakePlayerPlus/compare/v1.1.3...v1.1.4) (2026-06-27)


### Bug Fixes

* admin cannot open unowned fakeplayer inventory ([65b5a86](https://github.com/xiplugin/FakePlayerPlus/commit/65b5a8657331b75d00a60d997b4c28b060919242))

## [1.1.3](https://github.com/xiplugin/FakePlayerPlus/compare/v1.1.2...v1.1.3) (2026-06-27)


### Bug Fixes

* remove unused code ([d8aaf0d](https://github.com/xiplugin/FakePlayerPlus/commit/d8aaf0d86d2e312b836aa8ed9c777bdeb8b99c12))

## [1.1.2](https://github.com/xiplugin/FakePlayerPlus/compare/v1.1.1...v1.1.2) (2026-06-27)


### Bug Fixes

* rectify improper api usage and ([72052e5](https://github.com/xiplugin/FakePlayerPlus/commit/72052e5762f72e3bc362275c20ab60922d43362c))

## [1.1.1](https://github.com/xiplugin/FakePlayerPlus/compare/v1.1.0...v1.1.1) (2026-06-27)


### Bug Fixes

* unable to send chat messages without a chat plugin ([174239b](https://github.com/xiplugin/FakePlayerPlus/commit/174239b15b2b8333790565b50a58c16acbabf0d2))
* update getDestroyProgress implementation for v26_1_1 ([4512c07](https://github.com/xiplugin/FakePlayerPlus/commit/4512c0761e435396b3523b2f619c9c57a4b7147a))

## [1.1.0](https://github.com/xiplugin/FakePlayerPlus/compare/v1.0.1...v1.1.0) (2026-06-27)


### Features

* support importing fakeplayer data ([6c4e827](https://github.com/xiplugin/FakePlayerPlus/commit/6c4e827dbd61cda652193f4afbcf1a1cd7aee711))

## [1.0.1](https://github.com/xiplugin/FakePlayerPlus/compare/v1.0.0...v1.0.1) (2026-06-26)


### Bug Fixes

* reduce click callback lifetime to prevent memory leak ([c207cab](https://github.com/xiplugin/FakePlayerPlus/commit/c207cabebbff835cf19373e034853469927b6329))
