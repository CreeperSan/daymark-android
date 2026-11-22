import { DaymarkApi } from './DaymarkApi';
import { MockApi } from './MockApi';
import { RealApi } from './RealApi';

/**
 * 数据源开关。当前阶段使用本地 Mock，不改动页面与状态管理；
 * 接入真实服务时把 useMock 改为 false，并保留 Mock 供离线演示。
 */
export class ApiProvider {
  static readonly useMock: boolean = true;

  static api(): DaymarkApi {
    return ApiProvider.useMock ? MockApi.shared() : RealApi.shared();
  }
}
