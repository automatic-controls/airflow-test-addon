# AirflowDamperTesting

WebCTRL is a trademark of Automated Logic Corporation. Any other trademarks mentioned herein are the property of their respective owners.

## Overview

This WebCTRL add-on measures airflow response after closing and opening selected dampers on the geographic tree. Dampers are closed/opened sequentially to avoid triggering high static pressure alarms which could be incurred if all dampers are closed at once. Tests may be configured to use a delayed start if it is preferable to conduct testing at another time. After testing has concluded, all parameters will be reset to their initial values.

## Installation

1. If your WebCTRL server requires signed add-ons, copy the authenticating certificate [*ACES.cer*](https://github.com/automatic-controls/addon-dev-script/blob/main/ACES.cer?raw=true) to the *./addons* directory of your WebCTRL installation folder.

1. Install [AirflowDamperTesting.addon](https://github.com/automatic-controls/airflow-test-addon/releases/latest/download/AirflowDamperTesting.addon) using the WebCTRL interface.

1. Navigate to this add-on's main page to initiate an airflow damper test.

## Instructions

1. Select relevant locations on the geographic tree. The add-on will test all airflow microblocks under the selected locations.

1. Configure testing parameters. *Execution Delay* specifies how long to wait (in milliseconds) before initiating tests. *Damper Response Timeout* specifies how long to wait (in milliseconds) before measuring airflow response when the damper position is changed. When the damper is closed (locked to 0%), the measured airflow response must be sufficiently near to 0. When the damper is opened (locked to 100%), the measured airflow response must be sufficently near to the cooling max airflow design parameter (specified individually in each airflow microblock). The *Close Tolerance* and *Open Tolerance* percentages quantify the phrase *"sufficiently close"* in the previous two sentences.