#!/bin/bash
# 开发调试辅助脚本：构建 / 安装 / 启动 / 截图 / 点击
# 用法：
#   bash .tools/dev.sh build         构建并安装、重启应用
#   bash .tools/dev.sh shot a        截图到 .shots/a.jpeg
#   bash .tools/dev.sh dump          导出界面层级到 .shots/layout.json
#   bash .tools/dev.sh tapText 下一步  按文字找到控件并点击
#   bash .tools/dev.sh tap 542 2185  点击（真实像素坐标）
#   bash .tools/dev.sh log [行数]     查看应用日志
# 注意：必须设置 MSYS2_ARG_CONV_EXCL=*，否则 Git Bash 会把 /data/local/tmp 转成 Windows 路径

ROOT="C:/project/daymark-android/daymark-harmony"
HOST="/c/software/DevEco Studio/sdk/default/openharmony/toolchains"
export DEVECO_SDK_HOME="C:\\software\\DevEco Studio\\sdk"
export PATH="$HOST:/c/software/DevEco Studio/jbr/bin:/c/software/DevEco Studio/tools/node:$PATH"
export MSYS2_ARG_CONV_EXCL="*"

cd "$ROOT" || exit 1
mkdir -p .shots

case "$1" in
  build)
    "/c/software/DevEco Studio/tools/hvigor/bin/hvigorw.bat" assembleHap --mode module \
      -p product=default -p buildMode=debug --no-daemon 2>&1 | grep -E "ERROR|BUILD" | head -40
    hdc install -r entry/build/default/outputs/default/entry-default-unsigned.hap | tail -1
    hdc shell aa force-stop com.creepersan.daymark > /dev/null 2>&1
    hdc shell aa start -a EntryAbility -b com.creepersan.daymark > /dev/null
    sleep 2
    ;;
  shot)
    NAME="${2:-shot}"
    hdc shell snapshot_display -f /data/local/tmp/shot.jpeg > /dev/null
    hdc file recv /data/local/tmp/shot.jpeg ".shots/$NAME.jpeg" > /dev/null
    echo ".shots/$NAME.jpeg"
    ;;
  dump)
    hdc shell "uitest dumpLayout -p /data/local/tmp/layout.json" > /dev/null
    hdc file recv /data/local/tmp/layout.json .shots/layout.json > /dev/null
    ;;
  tapText)
    bash .tools/dev.sh dump
    XY=$(python .tools/find.py "$2")
    if [ "$XY" = "NOT_FOUND" ] || [ -z "$XY" ]; then
      echo "未找到控件：$2"
      exit 2
    fi
    echo "点击 $2 @ $XY"
    hdc shell "uitest uiInput click $XY"
    sleep 1
    ;;
  tap)
    hdc shell "uitest uiInput click $2 $3"
    sleep 1
    ;;
  swipe)
    hdc shell "uitest uiInput swipe $2 $3 $4 $5 400"
    sleep 1
    ;;
  log)
    hdc shell "hilog -x | grep -i Daymark | tail -${2:-20}"
    ;;
  *)
    echo "用法: build | shot <name> | dump | tapText <文字> | tap <x> <y> | swipe <x1> <y1> <x2> <y2> | log"
    ;;
esac
