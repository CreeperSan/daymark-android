/** 路由参数。统一用类承载，避免在 NavPathStack 中传递裸对象。 */
export class RouteParam {
  id: number = 0;

  constructor(id: number = 0) {
    this.id = id;
  }
}

/** 路由名称。 */
export class Routes {
  static readonly REGISTER: string = 'register';
  static readonly RESET: string = 'reset';
  static readonly CHANGE_PASSWORD: string = 'changePassword';
  static readonly SETTINGS: string = 'settings';
  static readonly ABOUT: string = 'about';
  static readonly EDIT: string = 'edit';
}
