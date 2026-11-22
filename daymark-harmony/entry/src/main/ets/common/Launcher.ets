import { common } from '@kit.AbilityKit';
import { Toast } from '../core/Toast';
import { I18n } from '../core/i18n/Strings';
import { Logger } from '../core/Logger';

/** 外部链接与邮件。失败时由调用方给出提示，不静默吞掉。 */
export class Launcher {
  static async openUrl(context: common.UIAbilityContext, url: string): Promise<void> {
    await context.startAbility({
      action: 'ohos.want.action.viewData',
      uri: url
    });
  }

  static async openMail(context: common.UIAbilityContext, address: string, subject: string, body: string): Promise<void> {
    Logger.event('打开邮件客户端：' + address);
    const uri: string = 'mailto:' + address + '?subject=' + encodeURIComponent(subject) + '&body=' +
      encodeURIComponent(body);
    await context.startAbility({
      action: 'ohos.want.action.sendToData',
      uri: uri
    });
  }

  static async open(context: common.UIAbilityContext, url: string): Promise<void> {
    try {
      Logger.event('打开外部链接：' + url);
      await Launcher.openUrl(context, url);
    } catch (err) {
      Logger.warn('打开外部链接失败：' + JSON.stringify(err));
      Toast.show(I18n.t('mailUnavailable'));
    }
  }
}
