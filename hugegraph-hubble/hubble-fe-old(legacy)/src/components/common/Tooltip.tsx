/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

import React from 'react';
import { observer } from 'mobx-react';
import TooltipTrigger, { Trigger } from 'react-popper-tooltip';
import 'react-popper-tooltip/dist/styles.css';

import type { Placement, Modifiers } from 'popper.js';

export interface TooltipProps {
  placement: Placement;
  tooltipShown?: boolean;
  trigger?: Trigger;
  modifiers?: Modifiers;
  tooltipWrapper: React.ReactNode;
  tooltipWrapperProps?: any;
  tooltipArrowClassName?: string;
  childrenWrapperElement?: 'div' | 'span' | 'img';
  childrenProps?: any;
  children?: React.ReactNode;
}

const Tooltip: React.FC<TooltipProps> = observer(
  ({
    placement,
    tooltipShown,
    trigger = 'click',
    modifiers,
    children,
    tooltipWrapper,
    tooltipWrapperProps,
    tooltipArrowClassName,
    childrenWrapperElement = 'span',
    childrenProps
  }) => (
    <TooltipTrigger
      trigger={trigger}
      placement={placement}
      tooltipShown={tooltipShown}
      modifiers={modifiers}
      tooltip={({
        arrowRef,
        tooltipRef,
        getArrowProps,
        getTooltipProps,
        placement
      }) => (
        <div
          {...getTooltipProps({
            ref: tooltipRef,
            ...tooltipWrapperProps
          })}
        >
          <div
            {...getArrowProps({
              ref: arrowRef,
              className: 'tooltip-arrow ' + (tooltipArrowClassName || ''),
              'data-placement': placement
            })}
          />
          {tooltipWrapper}
        </div>
      )}
    >
      {({ getTriggerProps, triggerRef }) => {
        const Tag = childrenWrapperElement;

        return (
          <Tag
            {...getTriggerProps({
              ref: triggerRef,
              ...childrenProps
            })}
          >
            {children}
          </Tag>
        );
      }}
    </TooltipTrigger>
  )
);

export default Tooltip;
