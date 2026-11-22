import { ApiError } from './network/ApiTypes';
import { I18n } from './i18n/Strings';

/** 把底层错误转成用户可读文案，服务端明确返回的 msg 优先展示。 */
export class ErrorText {
  static of(err: Object): string {
    const error: ApiError = err as ApiError;
    if (error === undefined || error === null) {
      return I18n.t('loadingFailed');
    }
    if (error.kind === 'network') {
      return I18n.t('networkFailed');
    }
    if (error.kind === 'auth') {
      return I18n.t('sessionExpired');
    }
    if (error.kind === 'invalid') {
      return I18n.t('invalidDataError');
    }
    if (error.kind === 'unavailable') {
      return I18n.t('featureUnavailable');
    }
    if (error.kind === 'server' && error.message !== undefined && error.message.length > 0) {
      return error.message;
    }
    return I18n.t('loadingFailed');
  }
}
