/* Copyright (c) 2022 The Brave Authors. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

#include "chrome/browser/browsing_data/chrome_browsing_data_remover_delegate.h"
#include "build/build_config.h"

// These files are not supposed to be used on Android and this guard should be
// fixed in the upstream. We will need to remove this block once it is done.
#if BUILDFLAG(IS_ANDROID)
#define CHROME_BROWSER_WEB_APPLICATIONS_COMMANDS_CLEAR_BROWSING_DATA_COMMAND_H_
#define CHROME_BROWSER_WEB_APPLICATIONS_WEB_APP_PROVIDER_H_
#define CHROME_BROWSER_WEB_APPLICATIONS_WEB_APP_UTILS_H_
#endif  // BUILDFLAG(IS_ANDROID)

#define kCompromisedCredentials \
  kIPFSCache:                   \
  return "kIPFSCache";          \
  case TracingDataType::kCompromisedCredentials

#include "src/chrome/browser/browsing_data/chrome_browsing_data_remover_delegate.cc"
#undef kCompromisedCredentials
#if BUILDFLAG(IS_ANDROID)
#undef CHROME_BROWSER_WEB_APPLICATIONS_WEB_APP_UTILS_H_
#undef CHROME_BROWSER_WEB_APPLICATIONS_WEB_APP_PROVIDER_H_
#undef CHROME_BROWSER_WEB_APPLICATIONS_COMMANDS_CLEAR_BROWSING_DATA_COMMAND_H_
#endif  // BUILDFLAG(IS_ANDROID)
