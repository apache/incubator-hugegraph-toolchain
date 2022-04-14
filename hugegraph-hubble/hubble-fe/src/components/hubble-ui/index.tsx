/* eslint-disable */
import {
  Transfer as TransferAntD,
  Popover,
  Pagination as PaginationAntD,
  Steps as StepsAntD,
  Progress as ProgressAntD,
  Checkbox as CheckboxAntD,
  Menu as MenuAntD,
  Spin,
  Breadcrumb as BreadcrumbAntD,
  Calendar as CalendarAntD,
  InputNumber,
  Switch as SwitchAntd,
  Table as TableAntD,
  Radio as RadioAntD,
  Tooltip as TooltipAntD,
  Alert as AlertAntD,
  Button as ButtonAntD,
  Modal as ModalAntD,
  Drawer as DrawerAntD,
  Input as InputAntD,
  message,
  Select as SelectAntD,
  Dropdown as DropdownAntd
} from 'antd';
import * as React from 'react';

// In order to make the project run, let the components of antd replace baiduUI
// Special Note: All check boxes can be selected, but some check boxes cannot see 
// the selection content, but it does not affect the submission. If you need to see
// the selection, you need to modify the value property of the corresponding Select
// component. There is no uniform modification here

const changeSize = (props: any): any => {
  let _size = props.size;
  if (_size === 'medium') {
    _size = 'middle';
  }
  if (!_size) {
    _size = 'small';
  }
  return {
    ...props,
    size: _size
  };
};

export const Alert = (props: any) => {
  return <AlertAntD {...props} message={props.content} />;
};

export const Button = (props: any) => {
  return <ButtonAntD {...changeSize(props)}>{props.children}</ButtonAntD>;
};

export const Modal = ModalAntD;

export const Drawer = (props: any) => {
  return <DrawerAntD {...props}>{props.children}</DrawerAntD>;
};

export const Input = (props: any) => {
  let _blur = () => {};
  if (props.originInputProps && props.originInputProps.onBlur) {
    _blur = props.originInputProps.onBlur;
  }
  // change e.value to eventTarget.currentTarget
  const _props = {
    ...props,
    onChange: (e: any) => {
      props.onChange({
        value: e.currentTarget.value
      });
    }
  };
  return (
    <div
      className={[
        'new-fc-one-input-all-container new-fc-one-input-all-container-medium',
        props.errorMessage ? 'new-fc-one-input-all-container-error' : ''
      ].join(' ')}
    >
      <InputAntD
        {...changeSize(_props)}
        style={{ width: props.width ? props.width : 'auto' }}
        onBlur={_blur}
      ></InputAntD>
      {props.errorMessage ? (
        <div className="new-fc-one-input-error new-fc-one-input-error-right">
          {props.errorMessage}
        </div>
      ) : (
        ''
      )}
    </div>
  );
};
Input.Search = (props: any) => {
  return (
    <InputAntD.Search
      {...changeSize(props)}
      style={{ width: props.width ? props.width : 'auto' }}
    ></InputAntD.Search>
  );
};

export const Message = {
  info: (data: any) => {
    message.info(data.content);
  },
  success: (data: any) => {
    message.success(data.content);
  },
  error: (data: any) => {
    message.error(data.content);
  },
  warning: (data: any) => {
    message.warning(data.content);
  },
  loading: (data: any) => {
    message.loading(data.content);
  }
};

export const Select: any = (props: any) => {
  return (
    <SelectAntD
      {...{ ...props, options: null }}
      placeholder={props.selectorName}
      style={{ width: props.width ? props.width : 'auto' }}
    >
      {props.children}
    </SelectAntD>
  );
};
Select.Option = SelectAntD.Option;
export const Tooltip: any = TooltipAntD;

export const Dropdown: any = {
  Button(props: any) {
    let _overlay: any = [];
    if (props.options) {
      _overlay = (
        <MenuAntD onClick={props.onHandleMenuClick}>
          {props.options.map((item: any) => (
            <MenuAntD.Item key={item.value} disabled={item.disabled}>
              {item.label}
            </MenuAntD.Item>
          ))}
        </MenuAntD>
      );
    }

    return (
      <DropdownAntd.Button
        {...{
          ...props,
          overlay: _overlay,
          options: null,
          onClick: props.onClickButton || (() => {})
        }}
      >
        {props.title}
      </DropdownAntd.Button>
    );
  }
};
export const Radio: any = RadioAntD;

export const Table: any = (props: any) => {
  let pagination = {};
  let pageChangerTag = false;
  if (props.pagination) {
    pagination = {
      ...props.pagination,
      onChange: (page: any, size: any) => {
        if (pageChangerTag) {
          return;
        }
        props.pagination.onPageNoChange({
          target: {
            value: page
          }
        });
      },
      showQuickJumper: props.pagination.showPageJumper,
      current: props.pagination.pageNo,
      onShowSizeChange: (e: any, size: any) => {
        pageChangerTag = true;
        props.pagination.onPageSizeChange({
          target: {
            value: size
          }
        });
        setTimeout(() => {
          pageChangerTag = false;
        });
      }
    };
  }
  let _handleChange: any = props.onChange || (() => {});
  // able to sort
  if (!props.onChange && props.onSortClick) {
    _handleChange = props.onSortClick;
  }
  return (
    <TableAntD {...{ ...props, pagination, onChange: _handleChange }}>
      {props.children}
    </TableAntD>
  );
};

export const Switch: any = (props: any) => {
  return (
    <SwitchAntd
      {...{ ...props, size: props.size === 'medium' ? 'default' : 'small' }}
      style={{ width: props.width ? props.width : 'auto' }}
    ></SwitchAntd>
  );
};

export const NumberBox: any = InputNumber;

export const Calendar = (props: any) => {
  props.onSelect = props.onSelectDay;
  return <CalendarAntD {...props}></CalendarAntD>;
};
export const Breadcrumb: any = BreadcrumbAntD;

export const Loading: any = Spin;

export const Menu: any = MenuAntD;

export const Checkbox: any = CheckboxAntD;

export const Progress: any = ProgressAntD;

export const Steps: any = StepsAntD;

export const Embedded: any = (props: any) => {
  return (
    <ModalAntD
      {...{ ...props, onCancel: props.onClose, width: 570, footer: [] }}
    >
      {props.children}
    </ModalAntD>
  );
};
export const Pagination = (props: any) => {
  return (
    <PaginationAntD
      {...changeSize(props)}
      showSizeChanger={props.showSizeChange}
      showQuickJumper={props.showPageJumper}
      current={props.pageNo}
      onChange={props.onPageNoChange}
    ></PaginationAntD>
  );
};

export const PopLayer: any = (props: any) => {
  return (
    <Popover {...{ ...props, title: props.overlay }}>{props.children}</Popover>
  );
};

export const Transfer: any = (props: any) => {
  const _treeName = props.treeName;
  const dataSource = props.dataSource || [];
  const allDataMap = props.allDataMap;
  if (allDataMap) {
    for (const key in allDataMap) {
      dataSource.push(allDataMap[key]);
    }
  }
  return (
    <TransferAntD
      {...{
        ...props,
        dataSource,
        oneWay: true,
        targetKeys: props.selectedList,
        onChange: (targetKeys) => {
          props.handleSelect(targetKeys);
        },
        titles: [`可选${_treeName}`, `已选${_treeName}`],
        render: (item) => `${item.title}`
      }}
    >
      {props.children}
    </TransferAntD>
  );
};
